package com.porrawc2026.app.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.porrawc2026.app.data.local.entity.GroupStandingEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class GroupStandingDao_Impl implements GroupStandingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GroupStandingEntity> __insertionAdapterOfGroupStandingEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public GroupStandingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGroupStandingEntity = new EntityInsertionAdapter<GroupStandingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `group_standings` (`teamId`,`groupLetter`,`position`,`played`,`won`,`drawn`,`lost`,`goalsFor`,`goalsAgainst`,`points`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GroupStandingEntity entity) {
        statement.bindString(1, entity.getTeamId());
        statement.bindString(2, entity.getGroupLetter());
        statement.bindLong(3, entity.getPosition());
        statement.bindLong(4, entity.getPlayed());
        statement.bindLong(5, entity.getWon());
        statement.bindLong(6, entity.getDrawn());
        statement.bindLong(7, entity.getLost());
        statement.bindLong(8, entity.getGoalsFor());
        statement.bindLong(9, entity.getGoalsAgainst());
        statement.bindLong(10, entity.getPoints());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM group_standings";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<GroupStandingEntity> standings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGroupStandingEntity.insert(standings);
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
  public Flow<List<GroupStandingEntity>> getAll() {
    final String _sql = "SELECT * FROM group_standings ORDER BY groupLetter, position";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"group_standings"}, new Callable<List<GroupStandingEntity>>() {
      @Override
      @NonNull
      public List<GroupStandingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTeamId = CursorUtil.getColumnIndexOrThrow(_cursor, "teamId");
          final int _cursorIndexOfGroupLetter = CursorUtil.getColumnIndexOrThrow(_cursor, "groupLetter");
          final int _cursorIndexOfPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "position");
          final int _cursorIndexOfPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "played");
          final int _cursorIndexOfWon = CursorUtil.getColumnIndexOrThrow(_cursor, "won");
          final int _cursorIndexOfDrawn = CursorUtil.getColumnIndexOrThrow(_cursor, "drawn");
          final int _cursorIndexOfLost = CursorUtil.getColumnIndexOrThrow(_cursor, "lost");
          final int _cursorIndexOfGoalsFor = CursorUtil.getColumnIndexOrThrow(_cursor, "goalsFor");
          final int _cursorIndexOfGoalsAgainst = CursorUtil.getColumnIndexOrThrow(_cursor, "goalsAgainst");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final List<GroupStandingEntity> _result = new ArrayList<GroupStandingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GroupStandingEntity _item;
            final String _tmpTeamId;
            _tmpTeamId = _cursor.getString(_cursorIndexOfTeamId);
            final String _tmpGroupLetter;
            _tmpGroupLetter = _cursor.getString(_cursorIndexOfGroupLetter);
            final int _tmpPosition;
            _tmpPosition = _cursor.getInt(_cursorIndexOfPosition);
            final int _tmpPlayed;
            _tmpPlayed = _cursor.getInt(_cursorIndexOfPlayed);
            final int _tmpWon;
            _tmpWon = _cursor.getInt(_cursorIndexOfWon);
            final int _tmpDrawn;
            _tmpDrawn = _cursor.getInt(_cursorIndexOfDrawn);
            final int _tmpLost;
            _tmpLost = _cursor.getInt(_cursorIndexOfLost);
            final int _tmpGoalsFor;
            _tmpGoalsFor = _cursor.getInt(_cursorIndexOfGoalsFor);
            final int _tmpGoalsAgainst;
            _tmpGoalsAgainst = _cursor.getInt(_cursorIndexOfGoalsAgainst);
            final int _tmpPoints;
            _tmpPoints = _cursor.getInt(_cursorIndexOfPoints);
            _item = new GroupStandingEntity(_tmpTeamId,_tmpGroupLetter,_tmpPosition,_tmpPlayed,_tmpWon,_tmpDrawn,_tmpLost,_tmpGoalsFor,_tmpGoalsAgainst,_tmpPoints);
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
  public Flow<List<GroupStandingEntity>> getByGroup(final String group) {
    final String _sql = "SELECT * FROM group_standings WHERE groupLetter = ? ORDER BY position";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, group);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"group_standings"}, new Callable<List<GroupStandingEntity>>() {
      @Override
      @NonNull
      public List<GroupStandingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTeamId = CursorUtil.getColumnIndexOrThrow(_cursor, "teamId");
          final int _cursorIndexOfGroupLetter = CursorUtil.getColumnIndexOrThrow(_cursor, "groupLetter");
          final int _cursorIndexOfPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "position");
          final int _cursorIndexOfPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "played");
          final int _cursorIndexOfWon = CursorUtil.getColumnIndexOrThrow(_cursor, "won");
          final int _cursorIndexOfDrawn = CursorUtil.getColumnIndexOrThrow(_cursor, "drawn");
          final int _cursorIndexOfLost = CursorUtil.getColumnIndexOrThrow(_cursor, "lost");
          final int _cursorIndexOfGoalsFor = CursorUtil.getColumnIndexOrThrow(_cursor, "goalsFor");
          final int _cursorIndexOfGoalsAgainst = CursorUtil.getColumnIndexOrThrow(_cursor, "goalsAgainst");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final List<GroupStandingEntity> _result = new ArrayList<GroupStandingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GroupStandingEntity _item;
            final String _tmpTeamId;
            _tmpTeamId = _cursor.getString(_cursorIndexOfTeamId);
            final String _tmpGroupLetter;
            _tmpGroupLetter = _cursor.getString(_cursorIndexOfGroupLetter);
            final int _tmpPosition;
            _tmpPosition = _cursor.getInt(_cursorIndexOfPosition);
            final int _tmpPlayed;
            _tmpPlayed = _cursor.getInt(_cursorIndexOfPlayed);
            final int _tmpWon;
            _tmpWon = _cursor.getInt(_cursorIndexOfWon);
            final int _tmpDrawn;
            _tmpDrawn = _cursor.getInt(_cursorIndexOfDrawn);
            final int _tmpLost;
            _tmpLost = _cursor.getInt(_cursorIndexOfLost);
            final int _tmpGoalsFor;
            _tmpGoalsFor = _cursor.getInt(_cursorIndexOfGoalsFor);
            final int _tmpGoalsAgainst;
            _tmpGoalsAgainst = _cursor.getInt(_cursorIndexOfGoalsAgainst);
            final int _tmpPoints;
            _tmpPoints = _cursor.getInt(_cursorIndexOfPoints);
            _item = new GroupStandingEntity(_tmpTeamId,_tmpGroupLetter,_tmpPosition,_tmpPlayed,_tmpWon,_tmpDrawn,_tmpLost,_tmpGoalsFor,_tmpGoalsAgainst,_tmpPoints);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
