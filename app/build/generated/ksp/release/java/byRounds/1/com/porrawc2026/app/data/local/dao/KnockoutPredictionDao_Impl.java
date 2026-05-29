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
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity;
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
public final class KnockoutPredictionDao_Impl implements KnockoutPredictionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<KnockoutPredictionEntity> __insertionAdapterOfKnockoutPredictionEntity;

  private final EntityDeletionOrUpdateAdapter<KnockoutPredictionEntity> __updateAdapterOfKnockoutPredictionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public KnockoutPredictionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfKnockoutPredictionEntity = new EntityInsertionAdapter<KnockoutPredictionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `knockout_predictions` (`matchNumber`,`round`,`homeTeamRef`,`awayTeamRef`,`winner`,`pointsEarned`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final KnockoutPredictionEntity entity) {
        statement.bindLong(1, entity.getMatchNumber());
        statement.bindString(2, entity.getRound());
        statement.bindString(3, entity.getHomeTeamRef());
        statement.bindString(4, entity.getAwayTeamRef());
        if (entity.getWinner() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getWinner());
        }
        statement.bindLong(6, entity.getPointsEarned());
      }
    };
    this.__updateAdapterOfKnockoutPredictionEntity = new EntityDeletionOrUpdateAdapter<KnockoutPredictionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `knockout_predictions` SET `matchNumber` = ?,`round` = ?,`homeTeamRef` = ?,`awayTeamRef` = ?,`winner` = ?,`pointsEarned` = ? WHERE `matchNumber` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final KnockoutPredictionEntity entity) {
        statement.bindLong(1, entity.getMatchNumber());
        statement.bindString(2, entity.getRound());
        statement.bindString(3, entity.getHomeTeamRef());
        statement.bindString(4, entity.getAwayTeamRef());
        if (entity.getWinner() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getWinner());
        }
        statement.bindLong(6, entity.getPointsEarned());
        statement.bindLong(7, entity.getMatchNumber());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM knockout_predictions";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<KnockoutPredictionEntity> predictions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfKnockoutPredictionEntity.insert(predictions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final KnockoutPredictionEntity prediction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfKnockoutPredictionEntity.handle(prediction);
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
  public Flow<List<KnockoutPredictionEntity>> getAll() {
    final String _sql = "SELECT * FROM knockout_predictions ORDER BY matchNumber";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"knockout_predictions"}, new Callable<List<KnockoutPredictionEntity>>() {
      @Override
      @NonNull
      public List<KnockoutPredictionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMatchNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "matchNumber");
          final int _cursorIndexOfRound = CursorUtil.getColumnIndexOrThrow(_cursor, "round");
          final int _cursorIndexOfHomeTeamRef = CursorUtil.getColumnIndexOrThrow(_cursor, "homeTeamRef");
          final int _cursorIndexOfAwayTeamRef = CursorUtil.getColumnIndexOrThrow(_cursor, "awayTeamRef");
          final int _cursorIndexOfWinner = CursorUtil.getColumnIndexOrThrow(_cursor, "winner");
          final int _cursorIndexOfPointsEarned = CursorUtil.getColumnIndexOrThrow(_cursor, "pointsEarned");
          final List<KnockoutPredictionEntity> _result = new ArrayList<KnockoutPredictionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KnockoutPredictionEntity _item;
            final int _tmpMatchNumber;
            _tmpMatchNumber = _cursor.getInt(_cursorIndexOfMatchNumber);
            final String _tmpRound;
            _tmpRound = _cursor.getString(_cursorIndexOfRound);
            final String _tmpHomeTeamRef;
            _tmpHomeTeamRef = _cursor.getString(_cursorIndexOfHomeTeamRef);
            final String _tmpAwayTeamRef;
            _tmpAwayTeamRef = _cursor.getString(_cursorIndexOfAwayTeamRef);
            final Integer _tmpWinner;
            if (_cursor.isNull(_cursorIndexOfWinner)) {
              _tmpWinner = null;
            } else {
              _tmpWinner = _cursor.getInt(_cursorIndexOfWinner);
            }
            final int _tmpPointsEarned;
            _tmpPointsEarned = _cursor.getInt(_cursorIndexOfPointsEarned);
            _item = new KnockoutPredictionEntity(_tmpMatchNumber,_tmpRound,_tmpHomeTeamRef,_tmpAwayTeamRef,_tmpWinner,_tmpPointsEarned);
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
  public Flow<List<KnockoutPredictionEntity>> getByRound(final String round) {
    final String _sql = "SELECT * FROM knockout_predictions WHERE round = ? ORDER BY matchNumber";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, round);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"knockout_predictions"}, new Callable<List<KnockoutPredictionEntity>>() {
      @Override
      @NonNull
      public List<KnockoutPredictionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMatchNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "matchNumber");
          final int _cursorIndexOfRound = CursorUtil.getColumnIndexOrThrow(_cursor, "round");
          final int _cursorIndexOfHomeTeamRef = CursorUtil.getColumnIndexOrThrow(_cursor, "homeTeamRef");
          final int _cursorIndexOfAwayTeamRef = CursorUtil.getColumnIndexOrThrow(_cursor, "awayTeamRef");
          final int _cursorIndexOfWinner = CursorUtil.getColumnIndexOrThrow(_cursor, "winner");
          final int _cursorIndexOfPointsEarned = CursorUtil.getColumnIndexOrThrow(_cursor, "pointsEarned");
          final List<KnockoutPredictionEntity> _result = new ArrayList<KnockoutPredictionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KnockoutPredictionEntity _item;
            final int _tmpMatchNumber;
            _tmpMatchNumber = _cursor.getInt(_cursorIndexOfMatchNumber);
            final String _tmpRound;
            _tmpRound = _cursor.getString(_cursorIndexOfRound);
            final String _tmpHomeTeamRef;
            _tmpHomeTeamRef = _cursor.getString(_cursorIndexOfHomeTeamRef);
            final String _tmpAwayTeamRef;
            _tmpAwayTeamRef = _cursor.getString(_cursorIndexOfAwayTeamRef);
            final Integer _tmpWinner;
            if (_cursor.isNull(_cursorIndexOfWinner)) {
              _tmpWinner = null;
            } else {
              _tmpWinner = _cursor.getInt(_cursorIndexOfWinner);
            }
            final int _tmpPointsEarned;
            _tmpPointsEarned = _cursor.getInt(_cursorIndexOfPointsEarned);
            _item = new KnockoutPredictionEntity(_tmpMatchNumber,_tmpRound,_tmpHomeTeamRef,_tmpAwayTeamRef,_tmpWinner,_tmpPointsEarned);
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
  public Object getTotalPoints(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT SUM(pointsEarned) FROM knockout_predictions";
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
