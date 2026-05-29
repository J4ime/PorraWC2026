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
import com.porrawc2026.app.data.local.entity.TeamEntity;
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
public final class TeamDao_Impl implements TeamDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TeamEntity> __insertionAdapterOfTeamEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public TeamDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTeamEntity = new EntityInsertionAdapter<TeamEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `teams` (`id`,`name`,`groupLetter`,`rank`,`flagEmoji`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TeamEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getGroupLetter());
        statement.bindLong(4, entity.getRank());
        statement.bindString(5, entity.getFlagEmoji());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM teams";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<TeamEntity> teams,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTeamEntity.insert(teams);
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
  public Flow<List<TeamEntity>> getAllTeams() {
    final String _sql = "SELECT * FROM teams ORDER BY groupLetter, rank";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"teams"}, new Callable<List<TeamEntity>>() {
      @Override
      @NonNull
      public List<TeamEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGroupLetter = CursorUtil.getColumnIndexOrThrow(_cursor, "groupLetter");
          final int _cursorIndexOfRank = CursorUtil.getColumnIndexOrThrow(_cursor, "rank");
          final int _cursorIndexOfFlagEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "flagEmoji");
          final List<TeamEntity> _result = new ArrayList<TeamEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TeamEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpGroupLetter;
            _tmpGroupLetter = _cursor.getString(_cursorIndexOfGroupLetter);
            final int _tmpRank;
            _tmpRank = _cursor.getInt(_cursorIndexOfRank);
            final String _tmpFlagEmoji;
            _tmpFlagEmoji = _cursor.getString(_cursorIndexOfFlagEmoji);
            _item = new TeamEntity(_tmpId,_tmpName,_tmpGroupLetter,_tmpRank,_tmpFlagEmoji);
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
  public Flow<List<TeamEntity>> getTeamsByGroup(final String group) {
    final String _sql = "SELECT * FROM teams WHERE groupLetter = ? ORDER BY rank";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, group);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"teams"}, new Callable<List<TeamEntity>>() {
      @Override
      @NonNull
      public List<TeamEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGroupLetter = CursorUtil.getColumnIndexOrThrow(_cursor, "groupLetter");
          final int _cursorIndexOfRank = CursorUtil.getColumnIndexOrThrow(_cursor, "rank");
          final int _cursorIndexOfFlagEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "flagEmoji");
          final List<TeamEntity> _result = new ArrayList<TeamEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TeamEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpGroupLetter;
            _tmpGroupLetter = _cursor.getString(_cursorIndexOfGroupLetter);
            final int _tmpRank;
            _tmpRank = _cursor.getInt(_cursorIndexOfRank);
            final String _tmpFlagEmoji;
            _tmpFlagEmoji = _cursor.getString(_cursorIndexOfFlagEmoji);
            _item = new TeamEntity(_tmpId,_tmpName,_tmpGroupLetter,_tmpRank,_tmpFlagEmoji);
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
