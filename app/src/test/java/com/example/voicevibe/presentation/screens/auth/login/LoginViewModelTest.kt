package com.example.voicevibe.presentation.screens.auth.login

import com.example.voicevibe.data.remote.dto.auth.AuthResponse
import com.example.voicevibe.data.remote.dto.auth.UserDto
import com.example.voicevibe.data.repository.AuthRepository
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        viewModel = LoginViewModel(authRepository)
    }

    @Test
    fun `login emits success event when credentials are valid`() = runTest {
        every { authRepository.login(any(), any()) } returns flow {
            emit(Resource.Loading())
            emit(Resource.Success(sampleAuthResponse()))
        }

        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("StrongPass123")

        val eventDeferred = async { viewModel.loginEvent.first() }

        viewModel.login()
        advanceUntilIdle()

        assertTrue(eventDeferred.await() is LoginEvent.Success)
        assertFalse(viewModel.uiState.value.isLoading)
        verify(exactly = 1) { authRepository.login("jane@example.com", "StrongPass123") }
    }

    @Test
    fun `login updates error state when repository returns error`() = runTest {
        every { authRepository.login(any(), any()) } returns flow {
            emit(Resource.Loading())
            emit(Resource.Error<AuthResponse>("Invalid email or password"))
        }

        viewModel.onEmailChanged("jane@example.com")
        viewModel.onPasswordChanged("StrongPass123")

        viewModel.login()
        advanceUntilIdle()

        assertEquals("Invalid email or password", viewModel.uiState.value.generalError)
        assertFalse(viewModel.uiState.value.isLoading)
        verify(exactly = 1) { authRepository.login("jane@example.com", "StrongPass123") }
    }

    @Test
    fun `login does not call repository when inputs are invalid`() = runTest {
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("123")

        viewModel.login()
        advanceUntilIdle()

        assertEquals("Email is required", viewModel.uiState.value.emailError)
        verify(exactly = 0) { authRepository.login(any(), any()) }
    }

    private fun sampleAuthResponse(): AuthResponse {
        val user = UserDto(
            id = 1,
            email = "jane@example.com",
            firstName = "Jane",
            lastName = "Doe",
            isActive = true,
            isVerified = true,
            dateJoined = "2024-01-01T00:00:00Z",
            profile = null
        )
        return AuthResponse(
            access = "access-token",
            refresh = "refresh-token",
            user = user
        )
    }
}
