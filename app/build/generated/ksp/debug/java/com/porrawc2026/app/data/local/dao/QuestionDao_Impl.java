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
import com.porrawc2026.app.data.local.entity.QuestionEntity;
import java.lang.Boolean;
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
public final class QuestionDao_Impl implements QuestionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<QuestionEntity> __insertionAdapterOfQuestionEntity;

  private final EntityDeletionOrUpdateAdapter<QuestionEntity> __updateAdapterOfQuestionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public QuestionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfQuestionEntity = new EntityInsertionAdapter<QuestionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `questions` (`id`,`text`,`predictedAnswer`,`correctAnswer`,`pointsEarned`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final QuestionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getText());
        final Integer _tmp = entity.getPredictedAnswer() == null ? null : (entity.getPredictedAnswer() ? 1 : 0);
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, _tmp);
        }
        final Integer _tmp_1 = entity.getCorrectAnswer() == null ? null : (entity.getCorrectAnswer() ? 1 : 0);
        if (_tmp_1 == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp_1);
        }
        statement.bindLong(5, entity.getPointsEarned());
      }
    };
    this.__updateAdapterOfQuestionEntity = new EntityDeletionOrUpdateAdapter<QuestionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `questions` SET `id` = ?,`text` = ?,`predictedAnswer` = ?,`correctAnswer` = ?,`pointsEarned` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final QuestionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getText());
        final Integer _tmp = entity.getPredictedAnswer() == null ? null : (entity.getPredictedAnswer() ? 1 : 0);
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, _tmp);
        }
        final Integer _tmp_1 = entity.getCorrectAnswer() == null ? null : (entity.getCorrectAnswer() ? 1 : 0);
        if (_tmp_1 == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp_1);
        }
        statement.bindLong(5, entity.getPointsEarned());
        statement.bindLong(6, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM questions";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<QuestionEntity> questions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfQuestionEntity.insert(questions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateQuestion(final QuestionEntity question,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfQuestionEntity.handle(question);
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
  public Flow<List<QuestionEntity>> getAllQuestions() {
    final String _sql = "SELECT * FROM questions ORDER BY id";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"questions"}, new Callable<List<QuestionEntity>>() {
      @Override
      @NonNull
      public List<QuestionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfPredictedAnswer = CursorUtil.getColumnIndexOrThrow(_cursor, "predictedAnswer");
          final int _cursorIndexOfCorrectAnswer = CursorUtil.getColumnIndexOrThrow(_cursor, "correctAnswer");
          final int _cursorIndexOfPointsEarned = CursorUtil.getColumnIndexOrThrow(_cursor, "pointsEarned");
          final List<QuestionEntity> _result = new ArrayList<QuestionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final QuestionEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            final Boolean _tmpPredictedAnswer;
            final Integer _tmp;
            if (_cursor.isNull(_cursorIndexOfPredictedAnswer)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(_cursorIndexOfPredictedAnswer);
            }
            _tmpPredictedAnswer = _tmp == null ? null : _tmp != 0;
            final Boolean _tmpCorrectAnswer;
            final Integer _tmp_1;
            if (_cursor.isNull(_cursorIndexOfCorrectAnswer)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getInt(_cursorIndexOfCorrectAnswer);
            }
            _tmpCorrectAnswer = _tmp_1 == null ? null : _tmp_1 != 0;
            final int _tmpPointsEarned;
            _tmpPointsEarned = _cursor.getInt(_cursorIndexOfPointsEarned);
            _item = new QuestionEntity(_tmpId,_tmpText,_tmpPredictedAnswer,_tmpCorrectAnswer,_tmpPointsEarned);
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
  public Object getTotalQuestionPoints(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT SUM(pointsEarned) FROM questions";
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
