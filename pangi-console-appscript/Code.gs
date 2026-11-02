const DEVICES_SHEET = 'Devices';
const CONFIG_SHEET = 'Config';
const OFFLINE_AFTER_MS = 180000;

function setupPangiConsole() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) throw new Error('Abre este script desde una Hoja de cálculo de Google.');

  let devices = ss.getSheetByName(DEVICES_SHEET);
  if (!devices) devices = ss.insertSheet(DEVICES_SHEET);
  devices.clear();
  devices.getRange(1, 1, 1, 18).setValues([[
    'deviceId','alias','fabricante','modelo','android','sdk','ultimaConexion',
    'estado','lat','lon','precision','bateria','cargando','gps',
    'comando','commandId','commandStatus','ultimoEvento'
  ]]);
  devices.setFrozenRows(1);
  devices.getRange(1,1,1,18).setFontWeight('bold');
  devices.autoResizeColumns(1,18);

  let config = ss.getSheetByName(CONFIG_SHEET);
  if (!config) config = ss.insertSheet(CONFIG_SHEET);
  config.clear();

  const deviceToken = randomToken_();
  const adminToken = randomToken_();

  config.getRange(1,1,4,2).setValues([
    ['CLAVE','VALOR'],
    ['DEVICE_TOKEN', deviceToken],
    ['ADMIN_TOKEN', adminToken],
    ['OFFLINE_AFTER_SECONDS', Math.round(OFFLINE_AFTER_MS / 1000)]
  ]);
  config.getRange(1,1,1,2).setFontWeight('bold');
  config.autoResizeColumns(1,2);

  return {
    ok: true,
    deviceToken,
    adminToken,
    message: 'Consola inicializada. Guarda estos tokens en un lugar seguro.'
  };
}

function doGet(e) {
  const action = String((e && e.parameter && e.parameter.action) || '').trim();

  if (!action) {
    return HtmlService
      .createTemplateFromFile('Index')
      .evaluate()
      .setTitle('Pangi Console')
      .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
  }

  try {
    if (action === 'ping') return apiPing_(e);
    if (action === 'heartbeat') return apiHeartbeat_(e);
    if (action === 'ack') return apiAck_(e);
    if (action === 'shutdown') return apiShutdown_(e);
    return json_({ok:false, error:'Acción no válida'});
  } catch (err) {
    return json_({ok:false, error:String(err && err.message ? err.message : err)});
  }
}

function apiPing_(e) {
  assertDeviceToken_(e);
  return json_({ok:true, serverTime:new Date().toISOString()});
}

function apiHeartbeat_(e) {
  assertDeviceToken_(e);
  const p = e.parameter || {};
  const id = clean_(p.deviceId);
  if (!id) throw new Error('deviceId requerido');

  const sheet = deviceSheet_();
  const row = findOrCreateDeviceRow_(sheet, id);
  const now = new Date();

  const previous = sheet.getRange(row, 1, 1, 18).getValues()[0];
  const command = clean_(previous[14]);
  const commandId = clean_(previous[15]);
  const commandStatus = clean_(previous[16]);

  const lat = numberOrBlank_(p.lat);
  const lon = numberOrBlank_(p.lon);
  const accuracy = numberOrBlank_(p.accuracy);

  const values = [
    id,
    clean_(p.label) || previous[1] || id,
    clean_(p.manufacturer),
    clean_(p.model),
    clean_(p.android),
    clean_(p.sdk),
    now,
    'ONLINE',
    lat === '' ? previous[8] : lat,
    lon === '' ? previous[9] : lon,
    accuracy === '' ? previous[10] : accuracy,
    numberOrBlank_(p.battery),
    clean_(p.charging) === '1' ? 'Sí' : 'No',
    clean_(p.gps) === '1' ? 'Activo' : 'Inactivo',
    command,
    commandId,
    commandStatus,
    'HEARTBEAT'
  ];

  sheet.getRange(row, 1, 1, 18).setValues([values]);

  const hasPending = command && commandId && commandStatus === 'PENDING';
  return json_({
    ok:true,
    serverTime:now.toISOString(),
    command: hasPending ? command : '',
    commandId: hasPending ? commandId : ''
  });
}

function apiAck_(e) {
  assertDeviceToken_(e);
  const p = e.parameter || {};
  const id = clean_(p.deviceId);
  const commandId = clean_(p.commandId);
  const status = clean_(p.status) || 'DONE';
  if (!id || !commandId) throw new Error('deviceId y commandId requeridos');

  const sheet = deviceSheet_();
  const row = findDeviceRow_(sheet, id);
  if (!row) return json_({ok:false, error:'Dispositivo no encontrado'});

  const currentId = clean_(sheet.getRange(row, 16).getValue());
  if (currentId !== commandId) {
    return json_({ok:false, error:'commandId no coincide'});
  }

  sheet.getRange(row, 17).setValue(status);
  sheet.getRange(row, 18).setValue('COMMAND_' + status);
  return json_({ok:true});
}

function apiShutdown_(e) {
  assertDeviceToken_(e);
  const p = e.parameter || {};
  const id = clean_(p.deviceId);
  if (!id) throw new Error('deviceId requerido');

  const sheet = deviceSheet_();
  const row = findOrCreateDeviceRow_(sheet, id);
  sheet.getRange(row, 7).setValue(new Date());
  sheet.getRange(row, 8).setValue('APAGÁNDOSE');
  sheet.getRange(row, 18).setValue('SHUTDOWN');
  return json_({ok:true});
}

function consoleList(adminToken) {
  assertAdminTokenValue_(adminToken);

  const sheet = deviceSheet_();
  const last = sheet.getLastRow();
  if (last < 2) return [];

  const rows = sheet.getRange(2, 1, last - 1, 18).getValues();
  const now = Date.now();

  return rows
    .filter(r => clean_(r[0]))
    .map(r => {
      const lastSeen = r[6] instanceof Date ? r[6].getTime() : 0;
      const online = lastSeen > 0 && (now - lastSeen) <= OFFLINE_AFTER_MS;
      return {
        deviceId: clean_(r[0]),
        alias: clean_(r[1]),
        manufacturer: clean_(r[2]),
        model: clean_(r[3]),
        android: clean_(r[4]),
        sdk: clean_(r[5]),
        lastSeen: lastSeen ? new Date(lastSeen).toISOString() : '',
        online,
        state: online ? 'ONLINE' : 'OFFLINE',
        lat: r[8] === '' ? null : Number(r[8]),
        lon: r[9] === '' ? null : Number(r[9]),
        accuracy: r[10] === '' ? null : Number(r[10]),
        battery: r[11] === '' ? null : Number(r[11]),
        charging: clean_(r[12]),
        gps: clean_(r[13]),
        command: clean_(r[14]),
        commandId: clean_(r[15]),
        commandStatus: clean_(r[16]),
        lastEvent: clean_(r[17])
      };
    });
}

function consoleCommand(adminToken, deviceId, command) {
  assertAdminTokenValue_(adminToken);
  const id = clean_(deviceId);
  const cmd = clean_(command).toUpperCase();

  if (!id) throw new Error('deviceId requerido');
  if (['LOCK','LOCATE'].indexOf(cmd) === -1) throw new Error('Comando no permitido');

  const sheet = deviceSheet_();
  const row = findDeviceRow_(sheet, id);
  if (!row) throw new Error('Dispositivo no encontrado');

  const commandId = Utilities.getUuid();
  sheet.getRange(row, 15, 1, 3).setValues([[cmd, commandId, 'PENDING']]);
  sheet.getRange(row, 18).setValue('COMMAND_QUEUED');

  return {ok:true, commandId};
}

function consoleRename(adminToken, deviceId, alias) {
  assertAdminTokenValue_(adminToken);
  const id = clean_(deviceId);
  const newAlias = clean_(alias);
  if (!id || !newAlias) throw new Error('Datos incompletos');

  const sheet = deviceSheet_();
  const row = findDeviceRow_(sheet, id);
  if (!row) throw new Error('Dispositivo no encontrado');
  sheet.getRange(row, 2).setValue(newAlias);
  return {ok:true};
}

function deviceSheet_() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheet = ss.getSheetByName(DEVICES_SHEET);
  if (!sheet) {
    setupPangiConsole();
    sheet = ss.getSheetByName(DEVICES_SHEET);
  }
  return sheet;
}

function findDeviceRow_(sheet, id) {
  const last = sheet.getLastRow();
  if (last < 2) return 0;
  const values = sheet.getRange(2,1,last-1,1).getValues();
  for (let i=0; i<values.length; i++) {
    if (clean_(values[i][0]) === id) return i + 2;
  }
  return 0;
}

function findOrCreateDeviceRow_(sheet, id) {
  const row = findDeviceRow_(sheet, id);
  if (row) return row;
  sheet.appendRow([id,'','','','','',new Date(),'ONLINE','','','','','','','','','','NEW_DEVICE']);
  return sheet.getLastRow();
}

function assertDeviceToken_(e) {
  const token = clean_(e && e.parameter && e.parameter.token);
  const expected = configValue_('DEVICE_TOKEN');
  if (!expected || token !== expected) throw new Error('Token de dispositivo inválido');
}

function assertAdminTokenValue_(token) {
  const expected = configValue_('ADMIN_TOKEN');
  if (!expected || clean_(token) !== expected) throw new Error('Token administrador inválido');
}

function configValue_(key) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(CONFIG_SHEET);
  if (!sheet) return '';
  const last = sheet.getLastRow();
  if (last < 2) return '';
  const rows = sheet.getRange(2,1,last-1,2).getValues();
  for (const row of rows) {
    if (clean_(row[0]) === key) return clean_(row[1]);
  }
  return '';
}

function numberOrBlank_(value) {
  const s = clean_(value);
  if (!s) return '';
  const n = Number(s);
  return Number.isFinite(n) ? n : '';
}

function clean_(value) {
  return value == null ? '' : String(value).trim();
}

function randomToken_() {
  return Utilities.getUuid().replace(/-/g,'') + Utilities.getUuid().replace(/-/g,'');
}

function json_(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
