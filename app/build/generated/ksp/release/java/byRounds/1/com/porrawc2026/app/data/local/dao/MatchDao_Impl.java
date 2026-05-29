package com.porrawc2026.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.porrawc2026.app.data.local.entity.MatchEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MatchDao_Impl implements MatchDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MatchEntity> __insertionAdapterOfMatchEntity;

  private final EntityDeletionOrUpdateAdapter<MatchEntity> __updateAdapterOfMatchEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final SharedSQLiteStatement __preparedStmtOfUpdateMatchResult;

  public MatchDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMatchEntity = new EntityInsertionAdapter<MatchEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `matches` (`id`,`groupName`,`matchday`,`dateTime`,`homeTeam`,`awayTeam`,`homeGoals`,`awayGoals`,`predictedHomeGoals`,`predictedAwayGoals`,`isKnockout`,`knockoutRound`,`matchNumber`,`pointsEarned`,`tvChannel`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MatchEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getGroupName());
        statement.bindString(3, entity.getMatchday());
        statement.bindString(4, entity.getDateTime());
        statement.bindString(5, entity.getHomeTeam());
        statement.bindString(6, entity.getAwayTeam());
        if (entity.getHomeGoals() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getHomeGoals());
        }
        if (entity.getAwayGoals() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getAwayGoals());
        }
        if (entity.getPredictedHomeGoals() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getPredictedHomeGoals());
        }
        if (entity.getPredictedAwayGoals() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getPredictedAwayGoals());
        }
        final int _tmp = entity.isKnockout() ? 1 : 0;
        statement.bindLong(11, _tmp);
        if (entity.getKnockoutRound() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getKnockoutRound());
        }
        if (entity.getMatchNumber() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getMatchNumber());
        }
        statement.bindLong(14, entity.getPointsEarned());
        statement.bindString(15, entity.getTvChannel());
      }
    };
    this.__updateAdapterOfMatchEntity = new EntityDeletionOrUpdateAdapter<MatchEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `matches` SET `id` = ?,`groupName` = ?,`matchday` = ?,`dateTime` = ?,`homeTeam` = ?,`awayTeam` = ?,`homeGoals` = ?,`awayGoals` = ?,`predictedHomeGoals` = ?,`predictedAwayGoals` = ?,`isKnockout` = ?,`knockoutRound` = ?,`matchNumber` = ?,`pointsEarned` = ?,`tvChannel` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MatchEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getGroupName());
        statement.bindString(3, entity.getMatchday());
        statement.bindString(4, entity.getDateTime());
        statement.bindString(5, entity.getHomeTeam());
        statement.bindString(6, entity.getAwayTeam());
        if (entity.getHomeGoals() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getHomeGoals());
        }
        if (entity.getAwayGoals() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getAwayGoals());
        }
        if (entity.getPredictedHomeGoals() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getPredictedHomeGoals());
        }
        if (entity.getPredictedAwayGoals() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getPredictedAwayGoals());
        }
        final int _tmp = entity.isKnockout() ? 1 : 0;
        statement.bindLong(11, _tmp);
        if (entity.getKnockoutRound() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getKnockoutRound());
        }
        if (entity.getMatchNumber() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getMatchNumber());
        }
        statement.bindLong(14, entity.getPointsEarned());
        statement.bindString(15, entity.getTvChannel());
        statement.bindLong(16, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM matches";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateMatchResult = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE matches SET homeGoals = ?, awayGoals = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<MatchEntity> matches,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMatchEntity.insert(matches);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMatch(final MatchEntity match, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMatchEntity.handle(match);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMatchResult(final int matchId, final int homeGoals, final int awayGoals,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateMatchResult.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, homeGoals);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, awayGoals);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, matchId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateMatchResult.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MatchEntity>> getAllGroupMatches() {
    final String _sql = "SELECT * FROM matches WHERE isKnockout = 0 ORDER BY id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"matches"}, new Callable<List<MatchEntity>>() {
      @Override
      @NonNull
      public List<MatchEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGroupName = CursorUtil.getColumnIndexOrThrow(_cursor, "groupName");
          final int _cursorIndexOfMatchday = CursorUtil.getColumnIndexOrThrow(_cursor, "matchday");
          final int _cursorIndexOfDateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTime");
          final int _cursorIndexOfHomeTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "homeTeam");
          final int _cursorIndexOfAwayTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "awayTeam");
          final int _cursorIndexOfHomeGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "homeGoals");
          final int _cursorIndexOfAwayGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "awayGoals");
          final int _cursorIndexOfPredictedHomeGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedHomeGoals");
          final int _cursorIndexOfPredictedAwayGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedAwayGoals");
          final int _cursorIndexOfIsKnockout = CursorUtil.getColumnIndexOrThrow(_cursor, "isKnockout");
          final int _cursorIndexOfKnockoutRound = CursorUtil.getColumnIndexOrThrow(_cursor, "knockoutRound");
          final int _cursorIndexOfMatchNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "matchNumber");
          final int _cursorIndexOfPointsEarned = CursorUtil.getColumnIndexOrThrow(_cursor, "pointsEarned");
          final int _cursorIndexOfTvChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "tvChannel");
          final List<MatchEntity> _result = new ArrayList<MatchEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MatchEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpGroupName;
            _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            final String _tmpMatchday;
            _tmpMatchday = _cursor.getString(_cursorIndexOfMatchday);
            final String _tmpDateTime;
            _tmpDateTime = _cursor.getString(_cursorIndexOfDateTime);
            final String _tmpHomeTeam;
            _tmpHomeTeam = _cursor.getString(_cursorIndexOfHomeTeam);
            final String _tmpAwayTeam;
            _tmpAwayTeam = _cursor.getString(_cursorIndexOfAwayTeam);
            final Integer _tmpHomeGoals;
            if (_cursor.isNull(_cursorIndexOfHomeGoals)) {
              _tmpHomeGoals = null;
            } else {
              _tmpHomeGoals = _cursor.getInt(_cursorIndexOfHomeGoals);
            }
            final Integer _tmpAwayGoals;
            if (_cursor.isNull(_cursorIndexOfAwayGoals)) {
              _tmpAwayGoals = null;
            } else {
              _tmpAwayGoals = _cursor.getInt(_cursorIndexOfAwayGoals);
            }
            final Integer _tmpPredictedHomeGoals;
            if (_cursor.isNull(_cursorIndexOfPredictedHomeGoals)) {
              _tmpPredictedHomeGoals = null;
            } else {
              _tmpPredictedHomeGoals = _cursor.getInt(_cursorIndexOfPredictedHomeGoals);
            }
            final Integer _tmpPredictedAwayGoals;
            if (_cursor.isNull(_cursorIndexOfPredictedAwayGoals)) {
              _tmpPredictedAwayGoals = null;
            } else {
              _tmpPredictedAwayGoals = _cursor.getInt(_cursorIndexOfPredictedAwayGoals);
            }
            final boolean _tmpIsKnockout;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsKnockout);
            _tmpIsKnockout = _tmp != 0;
            final String _tmpKnockoutRound;
            if (_cursor.isNull(_cursorIndexOfKnockoutRound)) {
              _tmpKnockoutRound = null;
            } else {
              _tmpKnockoutRound = _cursor.getString(_cursorIndexOfKnockoutRound);
            }
            final Integer _tmpMatchNumber;
            if (_cursor.isNull(_cursorIndexOfMatchNumber)) {
              _tmpMatchNumber = null;
            } else {
              _tmpMatchNumber = _cursor.getInt(_cursorIndexOfMatchNumber);
            }
            final int _tmpPointsEarned;
            _tmpPointsEarned = _cursor.getInt(_cursorIndexOfPointsEarned);
            final String _tmpTvChannel;
            _tmpTvChannel = _cursor.getString(_cursorIndexOfTvChannel);
            _item = new MatchEntity(_tmpId,_tmpGroupName,_tmpMatchday,_tmpDateTime,_tmpHomeTeam,_tmpAwayTeam,_tmpHomeGoals,_tmpAwayGoals,_tmpPredictedHomeGoals,_tmpPredictedAwayGoals,_tmpIsKnockout,_tmpKnockoutRound,_tmpMatchNumber,_tmpPointsEarned,_tmpTvChannel);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<MatchEntity>> getGroupMatches(final String group) {
    final String _sql = "SELECT * FROM matches WHERE groupName = ? AND isKnockout = 0 ORDER BY id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, group);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"matches"}, new Callable<List<MatchEntity>>() {
      @Override
      @NonNull
      public List<MatchEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGroupName = CursorUtil.getColumnIndexOrThrow(_cursor, "groupName");
          final int _cursorIndexOfMatchday = CursorUtil.getColumnIndexOrThrow(_cursor, "matchday");
          final int _cursorIndexOfDateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTime");
          final int _cursorIndexOfHomeTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "homeTeam");
          final int _cursorIndexOfAwayTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "awayTeam");
          final int _cursorIndexOfHomeGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "homeGoals");
          final int _cursorIndexOfAwayGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "awayGoals");
          final int _cursorIndexOfPredictedHomeGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedHomeGoals");
          final int _cursorIndexOfPredictedAwayGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedAwayGoals");
          final int _cursorIndexOfIsKnockout = CursorUtil.getColumnIndexOrThrow(_cursor, "isKnockout");
          final int _cursorIndexOfKnockoutRound = CursorUtil.getColumnIndexOrThrow(_cursor, "knockoutRound");
          final int _cursorIndexOfMatchNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "matchNumber");
          final int _cursorIndexOfPointsEarned = CursorUtil.getColumnIndexOrThrow(_cursor, "pointsEarned");
          final int _cursorIndexOfTvChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "tvChannel");
          final List<MatchEntity> _result = new ArrayList<MatchEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MatchEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpGroupName;
            _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            final String _tmpMatchday;
            _tmpMatchday = _cursor.getString(_cursorIndexOfMatchday);
            final String _tmpDateTime;
            _tmpDateTime = _cursor.getString(_cursorIndexOfDateTime);
            final String _tmpHomeTeam;
            _tmpHomeTeam = _cursor.getString(_cursorIndexOfHomeTeam);
            final String _tmpAwayTeam;
            _tmpAwayTeam = _cursor.getString(_cursorIndexOfAwayTeam);
            final Integer _tmpHomeGoals;
            if (_cursor.isNull(_cursorIndexOfHomeGoals)) {
              _tmpHomeGoals = null;
            } else {
              _tmpHomeGoals = _cursor.getInt(_cursorIndexOfHomeGoals);
            }
            final Integer _tmpAwayGoals;
            if (_cursor.isNull(_cursorIndexOfAwayGoals)) {
              _tmpAwayGoals = null;
            } else {
              _tmpAwayGoals = _cursor.getInt(_cursorIndexOfAwayGoals);
            }
            final Integer _tmpPredictedHomeGoals;
            if (_cursor.isNull(_cursorIndexOfPredictedHomeGoals)) {
              _tmpPredictedHomeGoals = null;
            } else {
              _tmpPredictedHomeGoals = _cursor.getInt(_cursorIndexOfPredictedHomeGoals);
            }
            final Integer _tmpPredictedAwayGoals;
            if (_cursor.isNull(_cursorIndexOfPredictedAwayGoals)) {
              _tmpPredictedAwayGoals = null;
            } else {
              _tmpPredictedAwayGoals = _cursor.getInt(_cursorIndexOfPredictedAwayGoals);
            }
            final boolean _tmpIsKnockout;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsKnockout);
            _tmpIsKnockout = _tmp != 0;
            final String _tmpKnockoutRound;
            if (_cursor.isNull(_cursorIndexOfKnockoutRound)) {
              _tmpKnockoutRound = null;
            } else {
              _tmpKnockoutRound = _cursor.getString(_cursorIndexOfKnockoutRound);
            }
            final Integer _tmpMatchNumber;
            if (_cursor.isNull(_cursorIndexOfMatchNumber)) {
              _tmpMatchNumber = null;
            } else {
              _tmpMatchNumber = _cursor.getInt(_cursorIndexOfMatchNumber);
            }
            final int _tmpPointsEarned;
            _tmpPointsEarned = _cursor.getInt(_cursorIndexOfPointsEarned);
            final String _tmpTvChannel;
            _tmpTvChannel = _cursor.getString(_cursorIndexOfTvChannel);
            _item = new MatchEntity(_tmpId,_tmpGroupName,_tmpMatchday,_tmpDateTime,_tmpHomeTeam,_tmpAwayTeam,_tmpHomeGoals,_tmpAwayGoals,_tmpPredictedHomeGoals,_tmpPredictedAwayGoals,_tmpIsKnockout,_tmpKnockoutRound,_tmpMatchNumber,_tmpPointsEarned,_tmpTvChannel);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<MatchEntity>> getKnockoutMatches() {
    final String _sql = "SELECT * FROM matches WHERE isKnockout = 1 ORDER BY matchNumber";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"matches"}, new Callable<List<MatchEntity>>() {
      @Override
      @NonNull
      public List<MatchEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGroupName = CursorUtil.getColumnIndexOrThrow(_cursor, "groupName");
          final int _cursorIndexOfMatchday = CursorUtil.getColumnIndexOrThrow(_cursor, "matchday");
          final int _cursorIndexOfDateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTime");
          final int _cursorIndexOfHomeTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "homeTeam");
          final int _cursorIndexOfAwayTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "awayTeam");
          final int _cursorIndexOfHomeGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "homeGoals");
          final int _cursorIndexOfAwayGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "awayGoals");
          final int _cursorIndexOfPredictedHomeGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedHomeGoals");
          final int _cursorIndexOfPredictedAwayGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedAwayGoals");
          final int _cursorIndexOfIsKnockout = CursorUtil.getColumnIndexOrThrow(_cursor, "isKnockout");
          final int _cursorIndexOfKnockoutRound = CursorUtil.getColumnIndexOrThrow(_cursor, "knockoutRound");
          final int _cursorIndexOfMatchNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "matchNumber");
          final int _cursorIndexOfPointsEarned = CursorUtil.getColumnIndexOrThrow(_cursor, "pointsEarned");
          final int _cursorIndexOfTvChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "tvChannel");
          final List<MatchEntity> _result = new ArrayList<MatchEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MatchEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpGroupName;
            _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            final String _tmpMatchday;
            _tmpMatchday = _cursor.getString(_cursorIndexOfMatchday);
            final String _tmpDateTime;
            _tmpDateTime = _cursor.getString(_cursorIndexOfDateTime);
            final String _tmpHomeTeam;
            _tmpHomeTeam = _cursor.getString(_cursorIndexOfHomeTeam);
            final String _tmpAwayTeam;
            _tmpAwayTeam = _cursor.getString(_cursorIndexOfAwayTeam);
            final Integer _tmpHomeGoals;
            if (_cursor.isNull(_cursorIndexOfHomeGoals)) {
              _tmpHomeGoals = null;
            } else {
              _tmpHomeGoals = _cursor.getInt(_cursorIndexOfHomeGoals);
            }
            final Integer _tmpAwayGoals;
            if (_cursor.isNull(_cursorIndexOfAwayGoals)) {
              _tmpAwayGoals = null;
            } else {
              _tmpAwayGoals = _cursor.getInt(_cursorIndexOfAwayGoals);
            }
            final Integer _tmpPredictedHomeGoals;
            if (_cursor.isNull(_cursorIndexOfPredictedHomeGoals)) {
              _tmpPredictedHomeGoals = null;
            } else {
              _tmpPredictedHomeGoals = _cursor.getInt(_cursorIndexOfPredictedHomeGoals);
            }
            final Integer _tmpPredictedAwayGoals;
            if (_cursor.isNull(_cursorIndexOfPredictedAwayGoals)) {
              _tmpPredictedAwayGoals = null;
            } else {
              _tmpPredictedAwayGoals = _cursor.getInt(_cursorIndexOfPredictedAwayGoals);
            }
            final boolean _tmpIsKnockout;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsKnockout);
            _tmpIsKnockout = _tmp != 0;
            final String _tmpKnockoutRound;
            if (_cursor.isNull(_cursorIndexOfKnockoutRound)) {
              _tmpKnockoutRound = null;
            } else {
              _tmpKnockoutRound = _cursor.getString(_cursorIndexOfKnockoutRound);
            }
            final Integer _tmpMatchNumber;
            if (_cursor.isNull(_cursorIndexOfMatchNumber)) {
              _tmpMatchNumber = null;
            } else {
              _tmpMatchNumber = _cursor.getInt(_cursorIndexOfMatchNumber);
            }
            final int _tmpPointsEarned;
            _tmpPointsEarned = _cursor.getInt(_cursorIndexOfPointsEarned);
            final String _tmpTvChannel;
            _tmpTvChannel = _cursor.getString(_cursorIndexOfTvChannel);
            _item = new MatchEntity(_tmpId,_tmpGroupName,_tmpMatchday,_tmpDateTime,_tmpHomeTeam,_tmpAwayTeam,_tmpHomeGoals,_tmpAwayGoals,_tmpPredictedHomeGoals,_tmpPredictedAwayGoals,_tmpIsKnockout,_tmpKnockoutRound,_tmpMatchNumber,_tmpPointsEarned,_tmpTvChannel);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<MatchEntity>> getAllMatches() {
    final String _sql = "SELECT * FROM matches ORDER BY id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"matches"}, new Callable<List<MatchEntity>>() {
      @Override
      @NonNull
      public List<MatchEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGroupName = CursorUtil.getColumnIndexOrThrow(_cursor, "groupName");
          final int _cursorIndexOfMatchday = CursorUtil.getColumnIndexOrThrow(_cursor, "matchday");
          final int _cursorIndexOfDateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTime");
          final int _cursorIndexOfHomeTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "homeTeam");
          final int _cursorIndexOfAwayTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "awayTeam");
          final int _cursorIndexOfHomeGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "homeGoals");
          final int _cursorIndexOfAwayGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "awayGoals");
          final int _cursorIndexOfPredictedHomeGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedHomeGoals");
          final int _cursorIndexOfPredictedAwayGoals = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedAwayGoals");
          final int _cursorIndexOfIsKnockout = CursorUtil.getColumnIndexOrThrow(_cursor, "isKnockout");
          final int _cursorIndexOfKnockoutRound = CursorUtil.getColumnIndexOrThrow(_cursor, "knockoutRound");
          final int _cursorIndexOfMatchNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "matchNumber");
          final int _cursorIndexOfPointsEarned = CursorUtil.getColumnIndexOrThrow(_cursor, "pointsEarned");
          final int _cursorIndexOfTvChannel = CursorUtil.getColumnIndexOrThrow(_cursor, "tvChannel");
          final List<MatchEntity> _result = new ArrayList<MatchEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MatchEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpGroupName;
            _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            final String _tmpMatchday;
            _tmpMatchday = _cursor.getString(_cursorIndexOfMatchday);
            final String _tmpDateTime;
            _tmpDateTime = _cursor.getString(_cursorIndexOfDateTime);
            final String _tmpHomeTeam;
            _tmpHomeTeam = _cursor.getString(_cursorIndexOfHomeTeam);
            final String _tmpAwayTeam;
            _tmpAwayTeam = _cursor.getString(_cursorIndexOfAwayTeam);
            final Integer _tmpHomeGoals;
            if (_cursor.isNull(_cursorIndexOfHomeGoals)) {
              _tmpHomeGoals = null;
            } else {
              _tmpHomeGoals = _cursor.getInt(_cursorIndexOfHomeGoals);
            }
            final Integer _tmpAwayGoals;
            if (_cursor.isNull(_cursorIndexOfAwayGoals)) {
              _tmpAwayGoals = null;
            } else {
              _tmpAwayGoals = _cursor.getInt(_cursorIndexOfAwayGoals);
            }
            final Integer _tmpPredictedHomeGoals;
            if (_cursor.isNull(_cursorIndexOfPredictedHomeGoals)) {
              _tmpPredictedHomeGoals = null;
            } else {
              _tmpPredictedHomeGoals = _cursor.getInt(_cursorIndexOfPredictedHomeGoals);
            }
            final Integer _tmpPredictedAwayGoals;
            if (_cursor.isNull(_cursorIndexOfPredictedAwayGoals)) {
              _tmpPredictedAwayGoals = null;
            } else {
              _tmpPredictedAwayGoals = _cursor.getInt(_cursorIndexOfPredictedAwayGoals);
            }
            final boolean _tmpIsKnockout;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsKnockout);
            _tmpIsKnockout = _tmp != 0;
            final String _tmpKnockoutRound;
            if (_cursor.isNull(_cursorIndexOfKnockoutRound)) {
              _tmpKnockoutRound = null;
            } else {
              _tmpKnockoutRound = _cursor.getString(_cursorIndexOfKnockoutRound);
            }
            final Integer _tmpMatchNumber;
            if (_cursor.isNull(_cursorIndexOfMatchNumber)) {
              _tmpMatchNumber = null;
            } else {
              _tmpMatchNumber = _cursor.getInt(_cursorIndexOfMatchNumber);
            }
            final int _tmpPointsEarned;
            _tmpPointsEarned = _cursor.getInt(_cursorIndexOfPointsEarned);
            final String _tmpTvChannel;
            _tmpTvChannel = _cursor.getString(_cursorIndexOfTvChannel);
            _item = new MatchEntity(_tmpId,_tmpGroupName,_tmpMatchday,_tmpDateTime,_tmpHomeTeam,_tmpAwayTeam,_tmpHomeGoals,_tmpAwayGoals,_tmpPredictedHomeGoals,_tmpPredictedAwayGoals,_tmpIsKnockout,_tmpKnockoutRound,_tmpMatchNumber,_tmpPointsEarned,_tmpTvChannel);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTotalMatchPoints(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT SUM(pointsEarned) FROM matches";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
