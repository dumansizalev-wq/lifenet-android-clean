package net.lifenet.core.messaging.qos

/**
 * QoS Seviyesi: Mesaj öncelik sınıflandırması
 * 
 * CRITICAL: PTT, SOS, sistem mesajları - Her zaman öncelikli
 * NORMAL: Metin, konum, bildirim - Standart öncelik
 * BULK: Log, senkronizasyon, dosya - Düşük öncelik, geciktirilebilir
 */
enum class QoSLevel(val priority: Int) {
    CRITICAL(100),  // En yüksek öncelik
    NORMAL(50),     // Orta öncelik
    BULK(10)        // En düşük öncelik
}
