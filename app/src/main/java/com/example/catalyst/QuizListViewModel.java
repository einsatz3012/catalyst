package com.example.catalyst;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class QuizListViewModel extends ViewModel implements com.example.catalyst.FirebaseRepository.OnFirestoreTaskComplete {

    private MutableLiveData<List<com.example.catalyst.QuizListModel>> quizListModelData = new MutableLiveData<>();

    public LiveData<List<com.example.catalyst.QuizListModel>> getQuizListModelData() {
        return quizListModelData;
    }

    private com.example.catalyst.FirebaseRepository firebaseRepository = new com.example.catalyst.FirebaseRepository(this);

    public QuizListViewModel() {
        firebaseRepository.getQuizData();
    }

    @Override
    public void quizListDataAdded(List<com.example.catalyst.QuizListModel> quizListModelsList) {
        quizListModelData.setValue(quizListModelsList);
    }

    @Override
    public void onError(Exception e) {

    }
}
