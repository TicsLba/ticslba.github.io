const VERSION = 'Tablet Escolar Relay 2.1';

function doGet() {
  return json({ok:true, service:VERSION, time:new Date().toISOString()});
}

function doPost(e) {
  try {
    const body = JSON.parse(e.postData.contents || '{}');
    const key = PropertiesService.getScriptProperties().getProperty('SCHOOL_KEY') || '';
    if (!key) return json({ok:false,error:'relay-not-configured'});
    const tag = sha256Hex(key).substring(0,12);
    if ((body.schoolTag || '') !== tag) return json({ok:false,error:'school-tag'});
    const op = String(body.op || '');
    if (op === 'telemetry') return telemetry(body,key,tag);
    if (op === 'devices') return devices(body,key,tag);
    if (op === 'command') return command(body,key,tag);
    return json({ok:false,error:'unknown-op'});
  } catch (err) {
    return json({ok:false,error:String(err)});
  }
}

function telemetry(b,key,tag) {
  const id=String(b.deviceId||''), payload=String(b.payload||''), ts=Number(b.ts||0), sig=String(b.sig||'');
  if (!id || !payload || Math.abs(Date.now()-ts)>120000) return json({ok:false,error:'bad-telemetry'});
  const expected=hmacHex(key,['telemetry',tag,id,ts,payload].join('\n'));
  if (!safeEq(sig,expected)) return json({ok:false,error:'auth'});
  const ss=sheet();
  const latest=getSheet(ss,'Latest',['device_id','ts','payload']);
  const log=getSheet(ss,'TelemetryLog',['ts','device_id','payload']);
  upsertLatest(latest,id,ts,payload);
  log.appendRow([ts,id,payload]);
  const commands=getPendingCommands(ss,id);
  return json({ok:true,commands:commands});
}

function devices(b,key,tag) {
  const ts=Number(b.ts||0), sig=String(b.sig||'');
  if (Math.abs(Date.now()-ts)>120000) return json({ok:false,error:'expired'});
  if (!safeEq(sig,hmacHex(key,['devices',tag,ts].join('\n')))) return json({ok:false,error:'auth'});
  const sh=getSheet(sheet(),'Latest',['device_id','ts','payload']);
  const values=sh.getDataRange().getValues();
  const out=[];
  for(let i=1;i<values.length;i++){
    const id=String(values[i][0]||'');if(!id)continue;
    out.push({deviceId:id,ts:Number(values[i][1]||0),payload:String(values[i][2]||'')});
  }
  return json({ok:true,devices:out});
}

function command(b,key,tag) {
  const id=String(b.deviceId||''), action=String(b.action||''), enc=String(b.enc||''), ts=Number(b.ts||0), sig=String(b.sig||''), reqSig=String(b.reqSig||'');
  if(!id||!action||!enc||Math.abs(Date.now()-ts)>120000)return json({ok:false,error:'bad-command'});
  const expectedCommand=hmacHex(key,[action,ts,enc].join('\n'));
  if(!safeEq(sig,expectedCommand))return json({ok:false,error:'command-auth'});
  const expectedReq=hmacHex(key,['command',tag,id,action,ts,enc,sig].join('\n'));
  if(!safeEq(reqSig,expectedReq))return json({ok:false,error:'request-auth'});
  const sh=getSheet(sheet(),'Commands',['created_at','device_id','action','ts','enc','sig','delivered_at']);
  sh.appendRow([Date.now(),id,action,ts,enc,sig,'']);
  return json({ok:true});
}

function getPendingCommands(ss,id){
  const sh=getSheet(ss,'Commands',['created_at','device_id','action','ts','enc','sig','delivered_at']);
  const range=sh.getDataRange(),values=range.getValues(),out=[],now=Date.now();
  for(let i=1;i<values.length;i++){
    if(String(values[i][1])!==id)continue;
    if(values[i][6])continue;
    const ts=Number(values[i][3]||0);
    if(now-ts>15*60*1000){sh.getRange(i+1,7).setValue('expired');continue;}
    out.push({action:String(values[i][2]),ts:ts,enc:String(values[i][4]),sig:String(values[i][5])});
    sh.getRange(i+1,7).setValue(now);
    if(out.length>=20)break;
  }
  return out;
}

function upsertLatest(sh,id,ts,payload){
  const values=sh.getDataRange().getValues();
  for(let i=1;i<values.length;i++){
    if(String(values[i][0])===id){sh.getRange(i+1,1,1,3).setValues([[id,ts,payload]]);return;}
  }
  sh.appendRow([id,ts,payload]);
}

function sheet(){
  const id=PropertiesService.getScriptProperties().getProperty('SHEET_ID')||'';
  if(!id)throw new Error('Missing SHEET_ID script property');
  return SpreadsheetApp.openById(id);
}
function getSheet(ss,name,headers){
  let sh=ss.getSheetByName(name);if(!sh){sh=ss.insertSheet(name);sh.appendRow(headers);}return sh;
}

function hmacHex(key,text){
  return bytesToHex(Utilities.computeHmacSha256Signature(text,key,Utilities.Charset.UTF_8));
}
function sha256Hex(text){return bytesToHex(Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,text,Utilities.Charset.UTF_8));}
function bytesToHex(bytes){return bytes.map(function(b){const v=(b<0?b+256:b);return ('0'+v.toString(16)).slice(-2);}).join('');}
function safeEq(a,b){a=String(a||'').toLowerCase();b=String(b||'').toLowerCase();if(a.length!==b.length)return false;let r=0;for(let i=0;i<a.length;i++)r|=a.charCodeAt(i)^b.charCodeAt(i);return r===0;}
function json(o){return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);}

function cleanupOldTelemetry(days){
  days=days||30;const sh=getSheet(sheet(),'TelemetryLog',['ts','device_id','payload']);const values=sh.getDataRange().getValues();const cutoff=Date.now()-days*86400000;
  for(let i=values.length-1;i>=1;i--)if(Number(values[i][0]||0)<cutoff)sh.deleteRow(i+1);
}
