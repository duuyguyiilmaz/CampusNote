package duygu.yilmaz.campusnote.data.model

/**
 * E-posta adresinin `@` işaretinden önceki kısmı — başka kullanıcılara gösterilen ad.
 *
 * Bu maskeleme eskiden yalnızca [PostAdapter]'da, satır içinde yapılıyordu;
 * liderlik tablosu ve not detayı aynı adresi tam hâliyle gösterdiği için maskeleme
 * hiçbir şey saklamıyordu. Tek bir yerde tanımlanınca üç ekran da aynı davranıyor.
 *
 * Kullanıcının kendi profilinde e-postası bilinçli olarak tam gösterilir:
 * maskelemenin amacı kişinin kendi adresini kendisinden saklamak değil.
 */
internal fun String.uploaderName(): String = substringBefore("@")
