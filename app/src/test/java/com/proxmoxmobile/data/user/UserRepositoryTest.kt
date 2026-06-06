package com.proxmoxmobile.data.user

import com.proxmoxmobile.data.model.ApiResponse
import com.proxmoxmobile.data.model.User
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRepositoryTest {
    @Test
    fun getUsers_filtersInvalidRowsAndSortsByUserid() = runBlocking {
        val repository = UserRepository(
            FakeUserApi(
                users = listOf(
                    user(userid = "root@pam"),
                    user(userid = ""),
                    user(userid = "admin@pve"),
                    user(userid = "backup@pbs")
                )
            )
        )

        val result = repository.getUsers()

        assertTrue(result is UserResult.Success)
        val users = (result as UserResult.Success).data
        assertEquals(listOf("admin@pve", "backup@pbs", "root@pam"), users.map { it.userid })
    }

    @Test
    fun getUsers_reportsMissingApiServiceAsNotAuthenticated() = runBlocking {
        val repository = UserRepository(ProxmoxUserApi { null })

        val result = repository.getUsers()

        assertTrue(result is UserResult.Error)
        assertEquals("Not authenticated", (result as UserResult.Error).message)
    }

    private class FakeUserApi(
        private val users: List<User> = listOf(user())
    ) : UserApi {
        override suspend fun getUsers(): ApiResponse<List<User>> {
            return ApiResponse(users)
        }
    }

    companion object {
        private fun user(
            userid: String = "root@pam",
            enable: Boolean = true
        ): User {
            return User(
                userid = userid,
                enable = enable,
                expire = null,
                firstname = "Proxmox",
                lastname = "Admin",
                email = "admin@example.test",
                comment = null
            )
        }
    }
}
