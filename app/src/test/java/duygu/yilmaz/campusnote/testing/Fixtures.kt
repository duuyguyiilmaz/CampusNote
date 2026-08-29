package duygu.yilmaz.campusnote.testing

import duygu.yilmaz.campusnote.data.model.AuthenticatedUser
import duygu.yilmaz.campusnote.data.model.NoteDraft
import duygu.yilmaz.campusnote.data.model.Post
import duygu.yilmaz.campusnote.data.model.UserProfile

/**
 * Testlerin ihtiyaç duymadığı alanları doldurmayan kısayollar; her test sadece
 * kendi ilgilendiği alanı vererek okunur kalıyor.
 */
fun authenticatedUser(
    uid: String = "uid-1",
    email: String = "ogrenci@ogr.akdeniz.edu.tr"
) = AuthenticatedUser(uid = uid, email = email)

fun userProfile(
    uid: String = "uid-1",
    email: String = "ogrenci@ogr.akdeniz.edu.tr",
    department: String = "Bilgisayar Mühendisliği",
    points: Long = 0L
) = UserProfile(id = uid, email = email, department = department, points = points)

fun post(
    id: String = "note-1",
    title: String = "Veri Yapıları Özeti",
    uploaderUid: String = "uid-2",
    authorEmail: String = "baskasi@ogr.akdeniz.edu.tr",
    department: String = "Bilgisayar Mühendisliği",
    ratingSum: Long = 0L,
    fileType: String = ""
) = Post(
    id = id,
    title = title,
    desc = "",
    authorEmail = authorEmail,
    department = department,
    timeMills = 0L,
    uploaderUid = uploaderUid,
    ratingSum = ratingSum,
    fileType = fileType
)

fun noteDraft(
    course: String = "BIL201",
    title: String = "Veri Yapıları Özeti",
    fileData: String = ""
) = NoteDraft(
    course = course,
    title = title,
    description = "",
    tag = "özet",
    fileName = "",
    fileType = "",
    fileData = fileData
)
