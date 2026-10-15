# Spawn Radius Rings

Xaero's World Map üzerinde, oyuncunun etrafındaki mob spawn ve despawn mesafelerini halka olarak gösteren bir Fabric modu.

*English: A Fabric mod for Minecraft 26.1.2 and 26.2 that draws mob spawn/despawn distance rings, spawning chunks and the world-spawn no-spawn zone on Xaero's World Map.*

## Özellikler

- **Halkalar:** 24 blok (spawn olmaz), 32 blok (rastgele despawn başlar), 64 blok (balıklar anında despawn) ve 128 blok (diğer mob'lar anında despawn). Mesafeler oyunun kendi değerlerinden (`MobCategory`) okunur.
- **Y kesiti:** Mesafeler küreseldir. Sabit bir Y seviyesi seçilirse halkalar o yükseklikte gerçekten geçerli olan yarıçapla çizilir.
- **Sabit merkez:** AFK noktası planlamak için merkez bir konuma sabitlenebilir.
- **Spawn chunk'ları:** Sunucunun mob spawn'ı çalıştırdığı chunk'lar gösterilir. Bunun için chunk merkezinin yatayda 128 bloktan yakın olması ve chunk'ın simülasyon mesafesi içinde kalması gerekir.
- **Dünya spawn alanı:** Dünya spawn noktasının 24 blok çevresi.
- Xaero tarzı harita butonları, bilgi paneli, etiketler, renk seçimi. Bilgi paneli Xaero'nun sol buton sütununu kapatmaz; halka etiketleri birbirinin ve panelin üstüne binmez (sığmayan etiket gösterilmez).
- Mod Menu desteği ve 19 dil.

## Gereksinimler

- Minecraft 26.1.2 (26.1.x) veya 26.2, Fabric Loader 0.19+, Java 25
- Fabric API (26.1.2 için 0.155.3+, 26.2 için 0.161.0+)
- Xaero's World Map 1.47.0+
- Xaero Lib (26.1.2 için 1.7.3+, 26.2 için 1.7.7+; Xaero's World Map'in içinde gelir, ayrıca kurmana gerek yok)
- İsteğe bağlı: Mod Menu

Bunların hepsi `fabric.mod.json` içinde bağımlılık olarak yazılıdır. Biri eksikse veya sürümü eskiyse oyun açılırken Fabric bunu adıyla söyler. Bağımlılık sürümleri derleme sırasında o Minecraft sürümünün `versions/` dosyasından alınır, yani modun derlendiği sürümlerle hep aynıdır.

## Derleme

Java 25 gerekir. Prism Launcher'ın indirdiği Java 25'i de kullanabilirsin:

```
set JAVA_HOME=%APPDATA%\PrismLauncher\java\java-runtime-epsilon
gradlew build
```

Bu Minecraft 26.1.2 jar'ını üretir. 26.2 için:

```
gradlew build -Pmc=26.2
```

Hazır jar'lar `build/libs/` klasörüne çıkar (`-sources` olmayan dosya), adında Minecraft sürümü yazar: `spawnradius-<sürüm>+26.1.2.jar` ve `spawnradius-<sürüm>+26.2.jar`. Oyununun sürümüne uyanı kur. Her derleme jar'ı ayrıca ana klasördeki `builds/<minecraft sürümü>/` klasörüne kopyalar ve oradaki eski sürümünü siler; ana klasördeki `build-all.bat` bütün modları her sürüm için tek seferde derler. `gradlew build` ayrıca `src/test` altındaki JUnit testlerini de çalıştırır.

### Desteklenen Minecraft sürümleri

Her sürümün bağımlılık sürümleri `versions/<minecraft>.properties` dosyasındadır (Fabric API, Mod Menu, Xaero's World Map, Xaero Lib). Kodun neredeyse tamamı ortaktır; Minecraft sürümleri arasında farklı olan birkaç çağrı (26.2'de ekranların `Minecraft.gui` üzerinden açılması gibi) `src/mc26.1/java` ve `src/mc26.2/java` altındaki `Compat` sınıfındadır. Yeni bir Minecraft sürümü eklemek için oraya bir `versions/` dosyası ve gerekirse bir `Compat` kopyası eklenir.

Bağımlılıklar şuralardan indirilir:
- Minecraft, Fabric Loader ve Fabric API: Fabric Maven
- Xaero's World Map ve Mod Menu: Modrinth Maven
- Xaero Lib: `maven.2b2t.vc/xaero` (XaeroPlus'ın da kullandığı depo)

Bu üç mod sadece derleme için kullanılır (`compileOnly`), jar'ın içine girmez.

### Xaero'nun harita ekranına neden mixin yok

Xaero'nun harita ekranı `GuiMap`'in çizim metodu çok büyüktür (~4700 komut, 180 yerel değişken). Bir sınıfa mixin uygulanınca Mixin bütün sınıfı yeniden yazar ve her metodun stack frame'lerini baştan hesaplar. Fabric Loader 0.20 ile gelen ASM 9.11 bu hesaba metot başına 100 milyon işlem sınırı koydu; `GuiMap` tek başına ~87 milyon işlem tutuyor, mixin kodu eklenince sınır aşılıyor ve oyun açılırken `LimitExceededException: Too many operations` ile çöküyor. Bu yüzden mod `GuiMap`'e hiç dokunmaz: butonlar ve tuşlar Fabric'in ekran olaylarıyla eklenir, harita konumu reflection ile okunur, halkalar da Xaero'nun her karede harita resmini çizdikten hemen sonra çağırdığı küçük `ImprovedFramebuffer.bindDefaultFramebuffer` metoduna eklenen mixin ile çizilir (çizim sırası eskisiyle aynı: haritanın üstünde, Xaero'nun yazılarının altında). Geliştirme ortamı bu tür sorunları erken yakalamak için Fabric Loader 0.20.0 ile çalışır; mod yine de 0.19+ ile çalışır.

## Proje yapısı

```
src/main/java/spawnradius/
  SpawnRadiusMod.java          giriş noktası, tuş atamaları, aç/kapat
  SpawnRadiusConfig.java       ayarlar (config/spawnradius.properties)
  SpawnRing.java               halkalar; mesafeler MobCategory'den okunur
  client/MapOverlayRenderer    harita koordinatları ve halka çizimi
  client/RingBatch             tüm şekilleri tek GUI öğesi olarak çizer (performans)
  client/SpawnChunkOverlay     spawn chunk gölgelendirmesi
  client/Legend                sol üstteki bilgi paneli
  client/SpawnRadiusScreen     ayarlar ekranı (kaydırılabilir liste; Varsayılanlar onay sorar)
  client/ui/                   ayar ekranı iskeleti; üç modda aynıdır, ekranlar aynı görünür
  client/XaeroIconButton       Xaero'nun buton sınıfıyla çizilen harita butonları
  client/WorldMapHook          Xaero'nun dünya haritasına halkaları, butonları ve tuşları ekler (Fabric ekran olaylarıyla)
  mixin/MixinImprovedFramebuffer  harita resmi çizildikten hemen sonra halkaları çizdirir
  mixin/GuiGraphicsExtractorAccessor
  compat/ModMenuIntegration
src/mc26.1/java, src/mc26.2/java   Minecraft sürümüne göre değişen birkaç çağrı (Compat)
versions/                      her Minecraft sürümü için bağımlılık sürümleri
src/main/resources/            fabric.mod.json, mixin ayarı, dil dosyaları, ikonlar
src/test/java/spawnradius/     JUnit testleri (halka mesafeleri, harita hesapları, ayarlar, dil dosyaları)
tools/MakeAssets.java          buton ikonlarını ve logoyu üretir: java tools/MakeAssets.java
tools/make_lang.py             dil dosyalarını üretir ve İngilizce'ye göre kontrol eder
```

Bir yazıyı değiştirmek veya dil eklemek için `tools/lang_*.py` dosyalarını düzenle, sonra `python3 tools/make_lang.py` çalıştır.

## Oyun koduna göre doğrulananlar (26.1.2)

- `NaturalSpawner`: oyuncuya uzaklığın karesi ≤ 576 (24²) olan yerlerde spawn olmaz. Dünya spawn noktasına 24 bloktan yakın yerlerde de olmaz.
- `MobCategory`: anında despawn mesafesi 128, balıklar için 64. Rastgele despawn mesafesi 32.
- `Mob.checkDespawn`: rastgele despawn için `noActionTime > 600` ve 1/800 ihtimal.
- `ChunkMap`: chunk merkezi yatayda 128 bloktan (16384 = 128²) yakın olmalı. Ayrıca chunk, simülasyon mesafesi içinde entity-ticking olmalı.

## Lisans

MIT, `LICENSE` dosyasına bak. Xaero's World Map ayrı bir mod ve kendi lisansına tabi. Bu proje Xaero'nun dosyalarını içermez.
