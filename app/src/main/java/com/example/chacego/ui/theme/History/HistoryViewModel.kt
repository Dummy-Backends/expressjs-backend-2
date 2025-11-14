package com.example.chacego.ui.theme.History

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chacego.data.GameHistory
import com.example.chacego.data.HistoryRetrofitClient
import kotlinx.coroutines.launch
import java.io.IOException
import retrofit2.HttpException

class HistoryViewModel : ViewModel() {

    private val historyApi = HistoryRetrofitClient.api

    var historyOfGames by mutableStateOf<List<GameHistory>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {

            isLoading = true
            errorMessage = null

            try {
                Log.d("HistoryViewModel", "Loading matches...")
                val matches = historyApi.getMatches()
                Log.d("HistoryViewModel", "Loaded ${matches.size} matches")

                // Convert MatchDto -> GameHistory (adapté à ton UI)
                historyOfGames = matches.map {
                    GameHistory(
                        timestamp = it.date,
                        result = when (it.resultat) {
                            "Victoire du chasseur" -> "win"
                            "Victoire de la proie" -> "loss"
                            "Match nul" -> "draw"
                            else -> "draw"
                        },
                        opponentId = it.role,
                        scoreChange = it.distance.toInt() // temp mapping
                    )
                }

            } catch (e: HttpException) {
                errorMessage = "Erreur serveur : ${e.code()}"
            } catch (e: IOException) {
                errorMessage = "Problème de connexion"
            } catch (e: Exception) {
                errorMessage = "Erreur inconnue : ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun refreshHistory() {
        loadHistory()
    }
}
