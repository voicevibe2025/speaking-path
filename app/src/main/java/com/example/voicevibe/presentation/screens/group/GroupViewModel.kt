package com.example.voicevibe.presentation.screens.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.GroupRepository
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.Group
import com.example.voicevibe.domain.model.GroupMember
import com.example.voicevibe.domain.model.GroupMessage
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _groupsState = MutableStateFlow<Resource<List<Group>>>(Resource.Loading())
    val groupsState: StateFlow<Resource<List<Group>>> = _groupsState.asStateFlow()

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup.asStateFlow()

    private val _joinGroupState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val joinGroupState: StateFlow<Resource<Boolean>> = _joinGroupState.asStateFlow()

    private val _membersState = MutableStateFlow<Resource<Pair<Group, List<GroupMember>>>>(Resource.Loading())
    val membersState: StateFlow<Resource<Pair<Group, List<GroupMember>>>> = _membersState.asStateFlow()

    private val _messagesState = MutableStateFlow<Resource<Triple<Group, List<GroupMessage>, Boolean>>>(Resource.Loading())
    val messagesState: StateFlow<Resource<Triple<Group, List<GroupMessage>, Boolean>>> = _messagesState.asStateFlow()

    private val _sendMessageState = MutableStateFlow<Resource<GroupMessage>?>(null)
    val sendMessageState: StateFlow<Resource<GroupMessage>?> = _sendMessageState.asStateFlow()

    private val _deleteMessageState = MutableStateFlow<Resource<Boolean>?>(null)
    val deleteMessageState: StateFlow<Resource<Boolean>?> = _deleteMessageState.asStateFlow()

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()

    init {
        // Load current user ID on initialization
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { resource ->
                if (resource is Resource.Success) {
                    _currentUserId.value = resource.data?.id?.toIntOrNull()
                }
            }
        }
    }

    /**
     * Load all available groups
     */
    fun loadGroups() {
        viewModelScope.launch {
            _groupsState.value = Resource.Loading()
            _groupsState.value = groupRepository.getGroups()
        }
    }

    /**
     * Select a group for preview
     */
    fun selectGroup(group: Group) {
        _selectedGroup.value = group
    }

    /**
     * Join a group (one-time selection)
     */
    fun joinGroup(groupId: Int) {
        viewModelScope.launch {
            _joinGroupState.value = Resource.Loading()
            val result = groupRepository.joinGroup(groupId)
            _joinGroupState.value = when (result) {
                is Resource.Success -> Resource.Success(true)
                is Resource.Error -> Resource.Error(result.message ?: "Failed to join group")
                is Resource.Loading -> Resource.Loading()
            }
        }
    }

    /**
     * Load members of current user's group
     */
    fun loadMyGroupMembers() {
        viewModelScope.launch {
            _membersState.value = Resource.Loading()
            _membersState.value = groupRepository.getMyGroupMembers()
        }
    }

    /**
     * Load messages from current user's group
     */
    fun loadMyGroupMessages(limit: Int = 50, offset: Int = 0) {
        viewModelScope.launch {
            _messagesState.value = Resource.Loading()
            _messagesState.value = groupRepository.getMyGroupMessages(limit, offset)
        }
    }

    /**
     * Silently refresh messages without showing loading state (prevents flicker)
     */
    private fun refreshMessagesQuietly(limit: Int = 50, offset: Int = 0) {
        viewModelScope.launch {
            val result = groupRepository.getMyGroupMessages(limit, offset)
            // Only update if successful, to avoid clearing current messages on error
            if (result is Resource.Success) {
                _messagesState.value = result
            }
        }
    }

    /**
     * Send a message to the group chat
     */
    fun sendMessage(message: String) {
        viewModelScope.launch {
            _sendMessageState.value = Resource.Loading()
            val result = groupRepository.sendMessage(message)
            _sendMessageState.value = result
            
            // Silently refresh to get the new message from server without flickering
            if (result is Resource.Success) {
                kotlinx.coroutines.delay(150) // Small delay to ensure server processed it
                refreshMessagesQuietly()
            }
        }
    }

    /**
     * Reset send message state
     */
    fun resetSendMessageState() {
        _sendMessageState.value = null
    }

    /**
     * Reset join group state
     */
    fun resetJoinGroupState() {
        _joinGroupState.value = Resource.Loading()
    }
    
    /**
     * Delete a message from the group chat
     */
    fun deleteMessage(messageId: Int) {
        viewModelScope.launch {
            _deleteMessageState.value = Resource.Loading()
            val result = groupRepository.deleteMessage(messageId)
            _deleteMessageState.value = result

            // Don't reload - UI already handles optimistic deletion
            // The message will stay removed in the optimistic UI
        }
    }

    fun resetDeleteMessageState() {
        _deleteMessageState.value = null
    }
}
