package duygu.yilmaz.campusnote.data.repository

import duygu.yilmaz.campusnote.data.model.UserProfile

interface UserRepository {

    suspend fun saveUser(user: UserProfile)

    /** Profil dokümanı yoksa null döner — kayıt yarıda kalmış kullanıcılar için. */
    suspend fun getUser(userId: String): UserProfile?
}
