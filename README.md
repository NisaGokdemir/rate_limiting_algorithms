# 🛡️ Rate Limiting Serüveni: Hangisini, Neden Seçmelisin?

Backend dünyasında sistemini korumak, sadece performanslı kod yazmak değil; aynı zamanda dış dünyadan gelen düzensiz trafiği dizginlemektir. Bu projede, hız sınırlama dünyasının "beşli çetesini" Java ile hayata geçirdik. İşte her birinin arkasındaki gerçek hikaye:



### Jeton Kovası (Token Bucket)
Modern sistemlerin (Nginx, API Gateway) favorisidir. Bir kovada jeton biriktiririz; istek gelince jeton harcanır. Sızdıran Kova'dan en büyük farkı "patlamalara" (burst) izin vermesidir. Kullanıcı bir süre hiç tıklamamışsa, kovada jeton birikir. Sonra aniden 10 isteği aynı saniyede atabilir. Ortalama hızı korurken, kullanıcıyı robot gibi tek tek tıklamaya zorlamadığı için daha "insancıl" bir deneyim sunar.
Genel amaçlı API sınırlamalarında bir numaradır. Kullanıcının bazen hızlı, bazen yavaş işlem yapmasına (örneğin bir sayfayı hızlıca yenilemesi) hoşgörü göstermek istediğin her yerde kullanabilirsin.

*Best Practice:* Jeton dolum işlemini arka planda bir thread ile yapmak yerine, her istek geldiğinde "aradaki zaman farkı kadar jeton ekle" mantığıyla (Lazy Refill) yapmalısın. Bu, işlemciyi yormaz.

*Türleri:* Strict Token Bucket (limit aşımında doğrudan reddeder) ve Waiting Token Bucket (jeton gelene kadar isteği biraz bekletir).

<img width="1938" height="1246" alt="image" src="https://github.com/user-attachments/assets/005ff463-e934-4ce7-ae2e-5cb869f6971e" />

### Sızdıran Kova (Leaky Bucket)
Bunu bir huni gibi düşünebilirsin. Üstten su (istekler) ne kadar düzensiz gelirse gelsin, alttaki küçük delikten hep aynı hızda akar. Eğer huni dolarsa, yeni gelen su dışarı taşar (yani istek reddedilir). Hedefteki veritabanın veya servislerin çok hassassa ve "bana saniyede 2 taneden fazla iş getirme" diyorsa, trafiği pürüzsüzleştirmek için en iyi dostun budur. Ancak dikkat; kullanıcıları kuyrukta beklettiği için biraz yavaşlık hissi yaratabilir.
Trafiği şekillendirmek (Traffic Shaping) istediğinde kullanabilirsin. Özellikle üçüncü parti bir servise (örneğin kargo API'si) istek atıyorsan ve o servis anlık yüklerde hata veriyorsa, trafiği pürüzsüzleştirmek için biçilmiş kaftandır.

*Best Practice:* Kuyruk (Queue) boyutunu çok büyük tutma. Çok büyük kuyruk, kullanıcının isteğinin çok geç işlenmesine neden olur; bu da "yavaşlık" şikayeti olarak döner.

*Türleri:* Shaping Leaky Bucket (çıkışı sabitler) ve Policing Leaky Bucket (limiti aşan paketleri anında siler).

<img width="1938" height="1246" alt="image" src="https://github.com/user-attachments/assets/c02adbdb-e6e1-4d62-8b0c-18067f401ce7" />

### Sabit Pencere (Fixed Window Counter)
Bu aslında en saf ve basit olanımız. Zamanı 1 dakikalık bloklara böler ve saymaya başlar. Ancak ciddi bir zaafı var: "Sınır Sorunu". Eğer limit dakikada 100 ise, bir kullanıcı saat 10:00:59'da 100 istek atıp, tam bir saniye sonra 10:01:00'da bir 100 tane daha atabilir. Teknik olarak limit aşılmamış gibi görünse de sistem 2 saniyede 200 isteğe maruz kalır. Hafiftir, RAM'i yormaz ama kurnaz kullanıcılara karşı biraz savunmasızdır.
Kaynakların (RAM/CPU) çok kısıtlı olduğu veya limitlerin çok esnek olduğu (örneğin: "saatte 10.000 istek") durumlarda kullanılabilir. "Sınır sorunu" bu kadar büyük rakamlarda çok fazla göze batmaz.

*Best Practice:* Dağıtık bir sistemde kullanıyorsan, her sunucu kendi sayacını tutarsa limitlerin anlamı kalmaz. Mutlaka Redis gibi merkezi bir atomik sayaç (INCR komutu gibi) kullanmalısın.

<img width="1938" height="1246" alt="image" src="https://github.com/user-attachments/assets/49ee3e69-e47e-4e08-a125-19fe019665b0" />

### Kayan Pencere Günlüğü (Sliding Window Log)
Hata payını sıfıra indirmek istiyorsan doğru yerdesin. Burada pencereler sabit değil, her istekle beraber kayar. Gelen her isteğin saniyesini tek tek deftere yazarız. Her yeni istekte defteri kontrol eder, "son 60 saniye içinde kaç kayıt var?" diye bakarız. Çok adaletlidir, "sınır sorunu" asla yaşanmaz. Ama bir bedeli var: Çok fazla istek gelirse defter çok kalınlaşır ve RAM'i doldurabilir. Kritik ve düşük trafikli güvenlik adımları için biçilmiş kaftandır.
"Günde sadece 1 kez SMS gönderebilirsin" veya "Şifre deneme limiti" gibi kritik güvenlik adımlarında kullanabilirsin. Hata payının kabul edilemeyeceği yerlerde paradan (RAM) kaçınmazsın.

*Best Practice:* RAM dolmasın diye, her allowRequest çağrısında sadece o anki limiti kontrol etmekle kalma; aynı zamanda pencerenin dışında kalmış çok eski kayıtları da agresif bir şekilde temizle.

<img width="1938" height="1246" alt="image" src="https://github.com/user-attachments/assets/c010653b-5414-4cbd-90a2-79e1039c5cde" />


### Kayan Pencere Sayacı (Sliding Window Counter)
Günlük tutmanın RAM maliyetiyle, sabit pencerelerin adaletsizliği arasında kalırsan bu orta yolu seçmelisin. Matematiksel bir ağırlıklı ortalama kullanır. Bir önceki dakikadaki yükün ne kadarının "hala geçerli" olduğunu hesaplar. Günlük tutmadan, sadece birkaç değişkenle pürüzsüz bir sınırlama sağlar. Hem ölçeklenebilir hem de oldukça adildir.
Ölçeklenebilir mikroservis mimarilerinde kullanılabilir. Hem adil olsun hem de sistemin canına okumasın diyorsan en dengeli aday budur.

*Best Practice:* Ağırlıklı ortalama hesabını yaparken zaman birimlerini (long vs double) dikkatli seç. Yuvarlama hataları çok yüksek trafik altında limitin sapmasına neden olabilir.

<img width="2386" height="1448" alt="image" src="https://github.com/user-attachments/assets/33a02de8-1903-4986-b076-c3b904015332" />

---

## 🧐 Karşılaştırma Özeti

| Yöntem | Burst (Hız Patlaması) | RAM Tüketimi | Hassasiyet |
| :--- | :---: | :---: | :--- |
| **Fixed Window** | ❌ | Çok Düşük | Düşük (Sınırda riskli) |
| **Leaky Bucket** | ❌ | Düşük | Yüksek (Sabit akış) |
| **Token Bucket** | ✅ | Düşük | Orta-Yüksek (Esnek) |
| **Sliding Log** | ❌ | Yüksek | En Yüksek (%100 Kesin) |
| **Sliding Counter**| ❌ | Düşük | Yüksek (Tahmini/Adil) |

### Küçük Bir Mühendislik Notu
Buradaki kodlar tek bir sunucu (in-memory) üzerinde çalışacak şekilde yazıldı. Eğer yarın bir gün sistemi dağıtık bir mimariye (microservices) taşırsan, sayaçları RAM yerine Redis gibi merkezi bir yerde tutman gerekecek. Aksi halde her sunucu kendi kafasına göre sayar ve limitlerin bir anlamı kalmaz.
