package com.porrawc2026.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.porrawc2026.app.data.local.dao.GroupStandingDao;
import com.porrawc2026.app.data.local.dao.GroupStandingDao_Impl;
import com.porrawc2026.app.data.local.dao.KnockoutPredictionDao;
import com.porrawc2026.app.data.local.dao.KnockoutPredictionDao_Impl;
import com.porrawc2026.app.data.local.dao.MatchDao;
import com.porrawc2026.app.data.local.dao.MatchDao_Impl;
import com.porrawc2026.app.data.local.dao.PlayerPredictionDao;
import com.porrawc2026.app.data.local.dao.PlayerPredictionDao_Impl;
import com.porrawc2026.app.data.local.dao.QuestionDao;
import com.porrawc2026.app.data.local.dao.QuestionDao_Impl;
import com.porrawc2026.app.data.local.dao.TeamDao;
import com.porrawc2026.app.data.local.dao.TeamDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile TeamDao _teamDao;

  private volatile MatchDao _matchDao;

  private volatile QuestionDao _questionDao;

  private volatile PlayerPredictionDao _playerPredictionDao;

  private volatile KnockoutPredictionDao _knockoutPredictionDao;

  private volatile GroupStandingDao _groupStandingDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `teams` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `groupLetter` TEXT NOT NULL, `rank` INTEGER NOT NULL, `flagEmoji` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `matches` (`id` INTEGER NOT NULL, `groupName` TEXT NOT NULL, `matchday` TEXT NOT NULL, `dateTime` TEXT NOT NULL, `homeTeam` TEXT NOT NULL, `awayTeam` TEXT NOT NULL, `homeGoals` INTEGER, `awayGoals` INTEGER, `predictedHomeGoals` INTEGER, `predictedAwayGoals` INTEGER, `isKnockout` INTEGER NOT NULL, `knockoutRound` TEXT, `matchNumber` INTEGER, `pointsEarned` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `questions` (`id` INTEGER NOT NULL, `text` TEXT NOT NULL, `predictedAnswer` INTEGER, `correctAnswer` INTEGER, `pointsEarned` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `player_predictions` (`rank` INTEGER NOT NULL, `playerName` TEXT NOT NULL, `predictedName` TEXT, `goalsScored` INTEGER NOT NULL, `pointsPerGoal` INTEGER NOT NULL, `pointsEarned` INTEGER NOT NULL, PRIMARY KEY(`rank`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `knockout_predictions` (`matchNumber` INTEGER NOT NULL, `round` TEXT NOT NULL, `homeTeamRef` TEXT NOT NULL, `awayTeamRef` TEXT NOT NULL, `winner` INTEGER, `pointsEarned` INTEGER NOT NULL, PRIMARY KEY(`matchNumber`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `group_standings` (`teamId` TEXT NOT NULL, `groupLetter` TEXT NOT NULL, `position` INTEGER NOT NULL, `played` INTEGER NOT NULL, `won` INTEGER NOT NULL, `drawn` INTEGER NOT NULL, `lost` INTEGER NOT NULL, `goalsFor` INTEGER NOT NULL, `goalsAgainst` INTEGER NOT NULL, `points` INTEGER NOT NULL, PRIMARY KEY(`teamId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '793ef87ca89c18dd30c261d8458114de')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `teams`");
        db.execSQL("DROP TABLE IF EXISTS `matches`");
        db.execSQL("DROP TABLE IF EXISTS `questions`");
        db.execSQL("DROP TABLE IF EXISTS `player_predictions`");
        db.execSQL("DROP TABLE IF EXISTS `knockout_predictions`");
        db.execSQL("DROP TABLE IF EXISTS `group_standings`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsTeams = new HashMap<String, TableInfo.Column>(5);
        _columnsTeams.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTeams.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTeams.put("groupLetter", new TableInfo.Column("groupLetter", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTeams.put("rank", new TableInfo.Column("rank", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTeams.put("flagEmoji", new TableInfo.Column("flagEmoji", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTeams = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTeams = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTeams = new TableInfo("teams", _columnsTeams, _foreignKeysTeams, _indicesTeams);
        final TableInfo _existingTeams = TableInfo.read(db, "teams");
        if (!_infoTeams.equals(_existingTeams)) {
          return new RoomOpenHelper.ValidationResult(false, "teams(com.porrawc2026.app.data.local.entity.TeamEntity).\n"
                  + " Expected:\n" + _infoTeams + "\n"
                  + " Found:\n" + _existingTeams);
        }
        final HashMap<String, TableInfo.Column> _columnsMatches = new HashMap<String, TableInfo.Column>(14);
        _columnsMatches.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("groupName", new TableInfo.Column("groupName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("matchday", new TableInfo.Column("matchday", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("dateTime", new TableInfo.Column("dateTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("homeTeam", new TableInfo.Column("homeTeam", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("awayTeam", new TableInfo.Column("awayTeam", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("homeGoals", new TableInfo.Column("homeGoals", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("awayGoals", new TableInfo.Column("awayGoals", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("predictedHomeGoals", new TableInfo.Column("predictedHomeGoals", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("predictedAwayGoals", new TableInfo.Column("predictedAwayGoals", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("isKnockout", new TableInfo.Column("isKnockout", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("knockoutRound", new TableInfo.Column("knockoutRound", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("matchNumber", new TableInfo.Column("matchNumber", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("pointsEarned", new TableInfo.Column("pointsEarned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMatches = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMatches = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMatches = new TableInfo("matches", _columnsMatches, _foreignKeysMatches, _indicesMatches);
        final TableInfo _existingMatches = TableInfo.read(db, "matches");
        if (!_infoMatches.equals(_existingMatches)) {
          return new RoomOpenHelper.ValidationResult(false, "matches(com.porrawc2026.app.data.local.entity.MatchEntity).\n"
                  + " Expected:\n" + _infoMatches + "\n"
                  + " Found:\n" + _existingMatches);
        }
        final HashMap<String, TableInfo.Column> _columnsQuestions = new HashMap<String, TableInfo.Column>(5);
        _columnsQuestions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuestions.put("text", new TableInfo.Column("text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuestions.put("predictedAnswer", new TableInfo.Column("predictedAnswer", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuestions.put("correctAnswer", new TableInfo.Column("correctAnswer", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuestions.put("pointsEarned", new TableInfo.Column("pointsEarned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysQuestions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesQuestions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoQuestions = new TableInfo("questions", _columnsQuestions, _foreignKeysQuestions, _indicesQuestions);
        final TableInfo _existingQuestions = TableInfo.read(db, "questions");
        if (!_infoQuestions.equals(_existingQuestions)) {
          return new RoomOpenHelper.ValidationResult(false, "questions(com.porrawc2026.app.data.local.entity.QuestionEntity).\n"
                  + " Expected:\n" + _infoQuestions + "\n"
                  + " Found:\n" + _existingQuestions);
        }
        final HashMap<String, TableInfo.Column> _columnsPlayerPredictions = new HashMap<String, TableInfo.Column>(6);
        _columnsPlayerPredictions.put("rank", new TableInfo.Column("rank", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayerPredictions.put("playerName", new TableInfo.Column("playerName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayerPredictions.put("predictedName", new TableInfo.Column("predictedName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayerPredictions.put("goalsScored", new TableInfo.Column("goalsScored", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayerPredictions.put("pointsPerGoal", new TableInfo.Column("pointsPerGoal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayerPredictions.put("pointsEarned", new TableInfo.Column("pointsEarned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlayerPredictions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlayerPredictions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlayerPredictions = new TableInfo("player_predictions", _columnsPlayerPredictions, _foreignKeysPlayerPredictions, _indicesPlayerPredictions);
        final TableInfo _existingPlayerPredictions = TableInfo.read(db, "player_predictions");
        if (!_infoPlayerPredictions.equals(_existingPlayerPredictions)) {
          return new RoomOpenHelper.ValidationResult(false, "player_predictions(com.porrawc2026.app.data.local.entity.PlayerPredictionEntity).\n"
                  + " Expected:\n" + _infoPlayerPredictions + "\n"
                  + " Found:\n" + _existingPlayerPredictions);
        }
        final HashMap<String, TableInfo.Column> _columnsKnockoutPredictions = new HashMap<String, TableInfo.Column>(6);
        _columnsKnockoutPredictions.put("matchNumber", new TableInfo.Column("matchNumber", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnockoutPredictions.put("round", new TableInfo.Column("round", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnockoutPredictions.put("homeTeamRef", new TableInfo.Column("homeTeamRef", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnockoutPredictions.put("awayTeamRef", new TableInfo.Column("awayTeamRef", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnockoutPredictions.put("winner", new TableInfo.Column("winner", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKnockoutPredictions.put("pointsEarned", new TableInfo.Column("pointsEarned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysKnockoutPredictions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesKnockoutPredictions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoKnockoutPredictions = new TableInfo("knockout_predictions", _columnsKnockoutPredictions, _foreignKeysKnockoutPredictions, _indicesKnockoutPredictions);
        final TableInfo _existingKnockoutPredictions = TableInfo.read(db, "knockout_predictions");
        if (!_infoKnockoutPredictions.equals(_existingKnockoutPredictions)) {
          return new RoomOpenHelper.ValidationResult(false, "knockout_predictions(com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity).\n"
                  + " Expected:\n" + _infoKnockoutPredictions + "\n"
                  + " Found:\n" + _existingKnockoutPredictions);
        }
        final HashMap<String, TableInfo.Column> _columnsGroupStandings = new HashMap<String, TableInfo.Column>(10);
        _columnsGroupStandings.put("teamId", new TableInfo.Column("teamId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupStandings.put("groupLetter", new TableInfo.Column("groupLetter", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupStandings.put("position", new TableInfo.Column("position", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupStandings.put("played", new TableInfo.Column("played", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupStandings.put("won", new TableInfo.Column("won", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupStandings.put("drawn", new TableInfo.Column("drawn", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupStandings.put("lost", new TableInfo.Column("lost", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupStandings.put("goalsFor", new TableInfo.Column("goalsFor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupStandings.put("goalsAgainst", new TableInfo.Column("goalsAgainst", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupStandings.put("points", new TableInfo.Column("points", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGroupStandings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGroupStandings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGroupStandings = new TableInfo("group_standings", _columnsGroupStandings, _foreignKeysGroupStandings, _indicesGroupStandings);
        final TableInfo _existingGroupStandings = TableInfo.read(db, "group_standings");
        if (!_infoGroupStandings.equals(_existingGroupStandings)) {
          return new RoomOpenHelper.ValidationResult(false, "group_standings(com.porrawc2026.app.data.local.entity.GroupStandingEntity).\n"
                  + " Expected:\n" + _infoGroupStandings + "\n"
                  + " Found:\n" + _existingGroupStandings);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "793ef87ca89c18dd30c261d8458114de", "80581616767bf260cede66899da1bf67");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "teams","matches","questions","player_predictions","knockout_predictions","group_standings");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `teams`");
      _db.execSQL("DELETE FROM `matches`");
      _db.execSQL("DELETE FROM `questions`");
      _db.execSQL("DELETE FROM `player_predictions`");
      _db.execSQL("DELETE FROM `knockout_predictions`");
      _db.execSQL("DELETE FROM `group_standings`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TeamDao.class, TeamDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MatchDao.class, MatchDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(QuestionDao.class, QuestionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PlayerPredictionDao.class, PlayerPredictionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(KnockoutPredictionDao.class, KnockoutPredictionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GroupStandingDao.class, GroupStandingDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TeamDao teamDao() {
    if (_teamDao != null) {
      return _teamDao;
    } else {
      synchronized(this) {
        if(_teamDao == null) {
          _teamDao = new TeamDao_Impl(this);
        }
        return _teamDao;
      }
    }
  }

  @Override
  public MatchDao matchDao() {
    if (_matchDao != null) {
      return _matchDao;
    } else {
      synchronized(this) {
        if(_matchDao == null) {
          _matchDao = new MatchDao_Impl(this);
        }
        return _matchDao;
      }
    }
  }

  @Override
  public QuestionDao questionDao() {
    if (_questionDao != null) {
      return _questionDao;
    } else {
      synchronized(this) {
        if(_questionDao == null) {
          _questionDao = new QuestionDao_Impl(this);
        }
        return _questionDao;
      }
    }
  }

  @Override
  public PlayerPredictionDao playerPredictionDao() {
    if (_playerPredictionDao != null) {
      return _playerPredictionDao;
    } else {
      synchronized(this) {
        if(_playerPredictionDao == null) {
          _playerPredictionDao = new PlayerPredictionDao_Impl(this);
        }
        return _playerPredictionDao;
      }
    }
  }

  @Override
  public KnockoutPredictionDao knockoutPredictionDao() {
    if (_knockoutPredictionDao != null) {
      return _knockoutPredictionDao;
    } else {
      synchronized(this) {
        if(_knockoutPredictionDao == null) {
          _knockoutPredictionDao = new KnockoutPredictionDao_Impl(this);
        }
        return _knockoutPredictionDao;
      }
    }
  }

  @Override
  public GroupStandingDao groupStandingDao() {
    if (_groupStandingDao != null) {
      return _groupStandingDao;
    } else {
      synchronized(this) {
        if(_groupStandingDao == null) {
          _groupStandingDao = new GroupStandingDao_Impl(this);
        }
        return _groupStandingDao;
      }
    }
  }
}
