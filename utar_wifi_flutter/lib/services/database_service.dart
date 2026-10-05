import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/user_config.dart';
import '../models/login_log.dart';
import '../models/portal_endpoint.dart';

class DatabaseService {
  static final DatabaseService _instance = DatabaseService._internal();
  factory DatabaseService() => _instance;
  DatabaseService._internal();

  Database? _db;

  Future<Database> get database async {
    if (_db != null) return _db!;
    _db = await _initDb();
    return _db!;
  }

  Future<Database> _initDb() async {
    String path = join(await getDatabasesPath(), 'utar_wifi.db');
    return await openDatabase(
      path,
      version: 1,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE config (
            id INTEGER PRIMARY KEY,
            username TEXT,
            password TEXT,
            targetSsid TEXT,
            autoLoginEnabled INTEGER,
            pollingIntervalSeconds INTEGER,
            isOnboardingCompleted INTEGER,
            lastLoginTime INTEGER,
            lastLoginSuccess INTEGER,
            lastStatusMessage TEXT
          )
        ''');
        await db.execute('''
          CREATE TABLE logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp INTEGER,
            status TEXT,
            message TEXT,
            ssid TEXT,
            portalUrl TEXT
          )
        ''');
        await db.execute('''
          CREATE TABLE endpoints (
            url TEXT PRIMARY KEY,
            lastUsedTime INTEGER
          )
        ''');
        // Insert default config
        await db.insert('config', UserConfig().toMap());
      },
    );
  }

  Future<UserConfig> getConfig() async {
    final db = await database;
    final maps = await db.query('config', where: 'id = ?', whereArgs: [1]);
    if (maps.isNotEmpty) {
      return UserConfig.fromMap(maps.first);
    }
    return UserConfig();
  }

  Future<void> saveConfig(UserConfig config) async {
    final db = await database;
    await db.update('config', config.toMap(), where: 'id = ?', whereArgs: [1]);
  }

  Future<void> addLog(LoginLog log) async {
    final db = await database;
    await db.insert('logs', log.toMap());
  }

  Future<List<LoginLog>> getAllLogs() async {
    final db = await database;
    final maps = await db.query('logs', orderBy: 'timestamp DESC');
    return maps.map((m) => LoginLog.fromMap(m)).toList();
  }

  Future<void> clearLogs() async {
    final db = await database;
    await db.delete('logs');
  }

  Future<void> saveEndpoint(PortalEndpoint endpoint) async {
    final db = await database;
    await db.insert('endpoints', endpoint.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<List<PortalEndpoint>> getAllEndpoints() async {
    final db = await database;
    final maps = await db.query('endpoints', orderBy: 'lastUsedTime DESC');
    return maps.map((m) => PortalEndpoint.fromMap(m)).toList();
  }

  Future<void> deleteEndpoint(String url) async {
    final db = await database;
    await db.delete('endpoints', where: 'url = ?', whereArgs: [url]);
  }
}
