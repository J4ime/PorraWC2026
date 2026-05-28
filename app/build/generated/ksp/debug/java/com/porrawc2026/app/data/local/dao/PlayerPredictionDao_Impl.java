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
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity;
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
public final class PlayerPredictionDao_Impl implements PlayerPredictionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PlayerPredictionEntity> __insertionAdapterOfPlayerPredictionEntity;

  private final EntityDeletionOrUpdateAdapter<PlayerPredictionEntity> __updateAdapterOfPlayerPredictionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public PlayerPredictionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlayerPredictionEntity = new EntityInsertionAdapter<PlayerPredictionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `player_predictions` (`rank`,`playerName`,`predictedName`,`goalsScored`,`pointsPerGoal`,`pointsEarned`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlayerPredictionEntity entity) {
        statement.bindLong(1, entity.getRank());
        statement.bindString(2, entity.getPlayerName());
        if (entity.getPredictedName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPredictedName());
        }
        statement.bindLong(4, entity.getGoalsScored());
        statement.bindLong(5, entity.getPointsPerGoal());
        statement.bindLong(6, entity.getPointsEarned());
      }
    };
    this.__updateAdapterOfPlayerPredictionEntity = new EntityDeletionOrUpdateAdapter<PlayerPredictionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `player_predictions` SET `rank` = ?,`playerName` = ?,`predictedName` = ?,`goalsScored` = ?,`pointsPerGoal` = ?,`pointsEarned` = ? WHERE `rank` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlayerPredictionEntity entity) {
        statement.bindLong(1, entity.getRank());
        statement.bindString(2, entity.getPlayerName());
        if (entity.getPredictedName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPredictedName());
        }
        statement.bindLong(4, entity.getGoalsScored());
        statement.bindLong(5, entity.getPointsPerGoal());
        statement.bindLong(6, entity.getPointsEarned());
        statement.bindLong(7, entity.getRank());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM player_predictions";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<PlayerPredictionEntity> predictions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlayerPredictionEntity.insert(predictions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final PlayerPredictionEntity prediction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPlayerPredictionEntity.handle(prediction);
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
  public Flow<List<PlayerPredictionEntity>> getAll() {
    final String _sql = "SELECT * FROM player_predictions ORDER BY rank";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"player_predictions"}, new Callable<List<PlayerPredictionEntity>>() {
      @Override
      @NonNull
      public List<PlayerPredictionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRank = CursorUtil.getColumnIndexOrThrow(_cursor, "rank");
          final int _cursorIndexOfPlayerName = CursorUtil.getColumnIndexOrThrow(_cursor, "playerName");
          final int _cursorIndexOfPredictedName = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedName");
          final int _cursorIndexOfGoalsScored = CursorUtil.getColumnIndexOrThrow(_cursor, "goalsScored");
          final int _cursorIndexOfPointsPerGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "pointsPerGoal");
          final int _cursorIndexOfPointsEarned = CursorUtil.getColumnIndexOrThrow(_cursor, "pointsEarned");
          final List<PlayerPredictionEntity> _result = new ArrayList<PlayerPredictionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlayerPredictionEntity _item;
            final int _tmpRank;
            _tmpRank = _cursor.getInt(_cursorIndexOfRank);
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final String _tmpPredictedName;
            if (_cursor.isNull(_cursorIndexOfPredictedName)) {
              _tmpPredictedName = null;
            } else {
              _tmpPredictedName = _cursor.getString(_cursorIndexOfPredictedName);
            }
            final int _tmpGoalsScored;
            _tmpGoalsScored = _cursor.getInt(_cursorIndexOfGoalsScored);
            final int _tmpPointsPerGoal;
            _tmpPointsPerGoal = _cursor.getInt(_cursorIndexOfPointsPerGoal);
            final int _tmpPointsEarned;
            _tmpPointsEarned = _cursor.getInt(_cursorIndexOfPointsEarned);
            _item = new PlayerPredictionEntity(_tmpRank,_tmpPlayerName,_tmpPredictedName,_tmpGoalsScored,_tmpPointsPerGoal,_tmpPointsEarned);
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
    final String _sql = "SELECT SUM(pointsEarned) FROM player_predictions";
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
