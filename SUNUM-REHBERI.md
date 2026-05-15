# SWE304 Project 4 — Sunum Rehberi (Basit Anlatim)

## Bu Proje Nedir? (Once konuyu anlayalim)

Yaptigimiz sey, basit bir web uygulamasini (**Patent Manager**) **otomatik olarak** sunucuya yukleyen bir sistem.

Hayal et: Sen kodda bir degisiklik yapip GitHub'a yukluyorsun. **Sen baska hicbir sey yapmadan**, koddaki degisiklik 2-3 dakika icinde calisan canli sisteme geliyor. Bunun adina **CI/CD** (Continuous Integration / Continuous Deployment) deniyor.

### Kullanilan araclar — kisaca her birinin gorevi

| Arac | Ne ise yariyor? |
|------|-----------------|
| **Spring Boot** | Java ile yazilmis web uygulamamiz (Patent Manager) |
| **Maven** | Java kodunu derleyip `.jar` dosyasi yapan arac |
| **Docker** | Uygulamayi "container" denilen tasinabilir kutuya koyuyor. Her makinede ayni sekilde calisir. |
| **DockerHub** | Docker container'larin saklandigi internet deposu (GitHub'in Docker versiyonu gibi) |
| **Kubernetes (K8s)** | Container'lari calistiran, yoneten ve dagilim yapan sistem |
| **Minikube** | Bilgisayarinda kucuk bir K8s kurulumu (gercek production cluster yerine) |
| **Jenkins** | Otomatik calisma sihirbazi. GitHub'a push olunca tetiklenip tum islemleri sirayla yapar. |
| **ngrok** | Lokal Jenkins'in internetten erisilebilmesini saglayan tunel (GitHub'in webhook gondermesi icin) |
| **GitHub Webhook** | "Birisi push yapti!" mesajini Jenkins'e gonderen mekanizma |

### Tum sistem nasil calisiyor? (Akis)

```
Sen kodu degistir → git push → GitHub
                                  ↓ (webhook)
                                ngrok tuneli
                                  ↓
                                Jenkins (lokal)
                                  ↓
        ┌─────────────────────────┴────────────────────────┐
        │                                                   │
   1. Kodu cek (git clone)                                  │
   2. Maven ile jar yap                                     │
   3. Docker image yap                                      │
   4. DockerHub'a giris yap                                 │
   5. Image'i DockerHub'a yukle  ────→  DockerHub          │
   6. Minikube'e "yeni image var, guncelle" de              │
                                                            │
                                                            ↓
                                              Minikube K8s cluster
                                                  (2 pod calisiyor)
                                                       ↓
                                              Tarayicidan goruyorsun
```

### Uygulamanin kendisi ne yapiyor?

**Patent Manager** — bilim insanlarinin patent kayitlarini tutan basit bir CRUD sitesi:

- **Authors** (Yazarlar): id, isim, adres
- **Patents** (Patentler): id, baslik, aciklama
- **Certifications** (Sertifikalar): hangi yazarin hangi patentine, ne zaman, kac yil sureli sertifika verildigi

Database olarak **H2 in-memory** kullaniyoruz (bilgisayarin RAM'inde tutulan basit bir DB, kurulum gerektirmez). Sayfa acildiginda Tesla, Edison, Curie ve onlarin patentleri seed olarak yukleniyor.

Sag ust kosede yesil **pod chip** var — bu, anlik olarak hangi K8s pod'unun cevap verdigini gosteriyor. Sayfayi her yenileyince farkli pod'a gidebilir (bu, K8s'in "load balancing" ozelligi).

---

## A) SUNUMDAN ONCE — Hazirlik Adimlari

> Bu adimlari **sunum gunu, bilgisayarin yeni acildiginda** sirayla yap. Hicbirini atlama.

### 1. Docker Desktop'i ac

Baslat menusu → **Docker Desktop** ikonuna tikla. Ekranin sag alt kosesindeki tepside Docker baliginin yesil olmasini bekle (~30 saniye).

**Kontrol komutu:**
```powershell
docker version
```
**Bu ne yapiyor?** Docker'in calisip calismadigini sorar. "Server" diye bir bolum gorursen tamamdir.

---

### 2. Minikube'u baslat (lokal K8s cluster)

```powershell
minikube start --driver=docker
```
**Bu ne yapiyor?** Bilgisayarinda kucuk bir K8s cluster ayaga kaldirir. (Driver olarak Docker kullaniyoruz cunku Windows'ta en stabilini.) Ilk acilmiyorsa 30-60 saniye surer.

**Kontrol:**
```powershell
minikube status
kubectl get nodes
```
> `host: Running` ve node'un `Ready` yazmasi gerekiyor.

---

### 3. Uygulamayi K8s'e yerlestir (eger zaten yoksa)

```powershell
cd C:\Users\ukbet\Desktop\DevopsProject4
kubectl apply -f k8s\deployment.yaml
kubectl apply -f k8s\service.yaml
kubectl scale deployment/patent-app-deployment --replicas=2
```
**Bu ne yapiyor?**
- `deployment.yaml`: K8s'e "patent-app container'ini calistir" der
- `service.yaml`: Uygulamaya disardan erisilecek bir kapi (port 30080) acar
- `scale --replicas=2`: 2 tane kopya (pod) calistirir → load balancing gosterimi icin

**Kontrol:**
```powershell
kubectl get pods
```
> `2/2` Running gormelisin (2 pod var, ikisi de calisiyor).

---

### 4. Jenkins'i baslat

```powershell
Get-Service jenkins
```
**Bu ne yapiyor?** Jenkins servisinin durumunu sorar. `Running` yaziyorsa tamam. Eger `Stopped` yaziyorsa **admin PowerShell'de** sunu calistir:
```powershell
Start-Service jenkins
```

Sonra tarayicidan ac: <http://localhost:8080> → kurulumda belirledigin admin parolasi ile giris yap.

---

### 5. ngrok tunelini ac (GitHub webhook icin)

```powershell
ngrok http 8080
```
**Bu ne yapiyor?** Lokal Jenkins'i (`localhost:8080`) internetten erisilebilir yapan gecici bir adres uretir. Cunku GitHub bir webhook gondermek istediginde lokal bilgisayarini direkt bulamaz, ngrok aracilik eder.

**Acilan ekranda:**
```
Forwarding   https://1234-5678.ngrok-free.dev → http://localhost:8080
```
Bu **`https://1234-5678.ngrok-free.dev`** adresi her ngrok aciliste **DEGISIR**. Bu adresi not et.

> Bu PowerShell penceresini **kapatma**, sunum boyunca acik kalmasi gerek.

---

### 6. GitHub'da webhook URL'sini guncelle

Ngrok'un verdigi yeni URL'yi GitHub'a soylemen lazim, yoksa push trigger calismaz.

1. Tarayicida ac: <https://github.com/UKBey/Devops_Project_4/settings/hooks>
2. Mevcut webhook'a tikla.
3. **Payload URL** alanina yapistir:
   ```
   https://YENI-NGROK-URL.ngrok-free.dev/github-webhook/
   ```
   > **Sondaki `/` karakteri sart!** Unutursan webhook calismaz.
4. En altta **Update webhook** butonuna bas. Yesil onay gelmeli.

---

### 7. Son kontrol

```powershell
minikube status
Get-Service jenkins
kubectl get pods
docker version
```
Hepsi calisiyorsa **HAZIRSIN**. Sunuma gec.

---

## B) SUNUM SIRASI — Hocaya Gosterilecek Adimlar

> **Hedef:** PDF'in (a) Jenkins kurulu, (b) Minikube kurulu, (c) CI/CD pipeline var, (d) K8s'de calisiyor, (e) Beklendigi gibi davraniyor — bes maddesini de tek bir akista ispatlamak.

### 1. Once sozlu olarak ozetle

Hocaya soyle:
> "Patent Manager adli bir Spring Boot web uygulamasi yaptim. Yazarlar, patentler ve aralarindaki sertifikalari yoneten basit bir CRUD app. Bunu Docker container'ina koydum, DockerHub'a yukleyip Minikube K8s cluster'da 2 replica halinde calistiriyorum. Jenkins ile bir CI/CD pipeline kurdum: GitHub'a push yapinca otomatik olarak yeni image build ediliyor, DockerHub'a yukleniyor ve K8s deploy ediliyor. Simdi her adimi gostericem."

---

### 2. K8s cluster'in calistigini goster

```powershell
minikube status
kubectl get nodes
kubectl get pods,svc
```
**Hocaya soyle:** "Iste lokal Minikube cluster'im calisiyor, 2 pod Running durumda, NodePort service var. (a) ve (b) maddeleri burada — Jenkins de Minikube de lokalde kurulu ve calisiyor."

---

### 3. Patent Manager uygulamasini ac (gercek calisan haliyle)

```powershell
minikube service patent-app-service
```
**Bu ne yapiyor?** Minikube'deki servise tarayicidan erisilecek gecici bir tunel acar ve tarayiciyi otomatik baslatir.

**Hocaya goster:**
- **Authors** tab → Tesla, Edison, Curie hazir gorunuyor
- **Patents** tab → 4 patent var
- **Certifications** tab → 4 sertifika (her biri yazar + patent + tarih + sure)
- Sag ust kosedeki **yesil pod chip** → "iste su anda bu istegi karsilayan pod'un adi"
- Yeni Author ekle: form doldur → Ekle → tabloya gelir
- Yeni Certification ekle: dropdown'lardan sec, tarih + sure → Ekle

**Hocaya soyle:** "(e) maddesi — uygulama beklendigi gibi calisiyor, CRUD yapabiliyorum."

> **Bu sekmeyi acik birak**, sunum boyunca buraya geri donecegiz.

---

### 4. Jenkins pipeline'i goster (push'tan ONCE)

Tarayicida ac: <http://localhost:8080/job/patent-app-pipeline/>

**Hocaya goster:**
- **Stage View**'da onceki build'lerin yesil kutucuklarini goster — 6 stage var:
  1. Clone (kodu git'ten cek)
  2. Build (jar yap)
  3. Docker (image yap)
  4. Login (DockerHub'a giris)
  5. Push (image'i yukle)
  6. Deploy (K8s'e ilet)
- **Son build numarasini AKLINDA TUT** (ornek: #5). Birazdan push yapinca #6 baslayacak.
- Bir build'e tikla → Console Output → loglarini goster.

**Hocaya soyle:** "(c) maddesi — pipeline'im 6 stage ile calisiyor, su ana kadar 5 kere otomatik calismis."

---

### 5. ⭐ EN ONEMLI ADIM: Canli Push + Deploy Demosu

**Bu tek adim PDF'in (c), (d), (e) maddelerini ayni anda ispatlar.**

**Amac:** Sen kodda kucuk bir degisiklik yap, push et — Jenkins kendiliginden tetiklenip tum pipeline'i kossun, sonunda yeni hali tarayicida gozuksun.

#### Adim 5.1 — Kodda gorunur bir degisiklik yap

```powershell
cd C:\Users\ukbet\Desktop\DevopsProject4
(Get-Content src\main\resources\static\index.html) -replace 'SWE304 Project 4 - Authors / Patents / Certifications', 'SWE304 Project 4 - LIVE DEMO BUILD' | Set-Content src\main\resources\static\index.html
```
**Bu ne yapiyor?** Index.html dosyasindaki sayfa altbasligini "LIVE DEMO BUILD" olarak degistirir. Goz ile rahat farkedilir.

#### Adim 5.2 — Push et

```powershell
git add .
git commit -m "Demo: trigger pipeline from push"
git push
```
**Bu ne yapiyor?** Degisikligi GitHub'a gonderir. GitHub webhook ile Jenkins'i tetikler.

#### Adim 5.3 — Jenkins sayfasini yenile

Tarayicida Jenkins'e geri don, sayfayi yenile (F5).

**Hocaya goster:**
- Yeni build (#6) **kendiliginden basladi** — sen tiklamadin!
- Build detayina tikla → "Started by GitHub push by UKBey" mesajini goster
- Stage View'da yesil kutucuklarin sirayla dolmasini izleyin (~2-3 dakika)

**Hocaya soyle:** "Iste hicbir manuel islem yok — sadece push yaptim, geri kalan her sey kendiliginden oluyor."

#### Adim 5.4 — Pipeline bittikten sonra Patent Manager sekmesini yenile

Tarayicida adim 3'teki Patent Manager sekmesine don, F5 ile yenile.

**Hocaya goster:** Baslik altinda "LIVE DEMO BUILD" yazisi gozukmeli — degisiklik canliya cikti.

**Hocaya soyle:** "Pipeline gercekten kodumu DockerHub'a yukledi, K8s yeni image'i cekti, eski pod'lari oldurup yeni pod'lar acti — hepsi otomatik. (c), (d), (e) maddelerinin ucu birden burada kanitlandi."

---

### 6. DockerHub'da yeni image'i goster

Tarayici: <https://hub.docker.com/r/ukbey/patent-app/tags>

**Hocaya goster:** `latest` tag'inin "Last pushed" tarihi **az once**. Yani gercekten Jenkins push'ladi.

---

### 7. Load balancing'i goster

Patent Manager sekmesine don. Sag ust kosedeki **pod chip**'e dikkat et.

**F5 ile sayfayi pes pese 6-7 kere yenile.**

**Hocaya goster:** Pod chip'teki isim iki farkli pod arasinda gidip geliyor. Mesela:
- `patent-app-deployment-abc123-xyz` 
- `patent-app-deployment-def456-uvw`

**Hocaya soyle:** "K8s'in Service'i her HTTP isteji farkli pod'a yonlendiriyor — buna kube-proxy round-robin deniyor. 2 pod arasinda yuk paylastiriliyor."

---

### 8. Cluster icinden de kanit (busybox testi)

```powershell
kubectl run lb-test --rm -i --restart=Never --image=busybox -- sh -c "for i in 1 2 3 4 5 6 7 8 9 10; do wget -qO- http://patent-app-service:8080/api/hello; echo; done"
```
**Bu ne yapiyor?** K8s cluster'in **icinden** kucuk bir busybox container ayaga kaldirir, 10 istek atip sonuclari basar.

**Hocaya goster:** Her cevapta "served by pod: ..." farkli — yani cluster icinden de load balancing calisiyor.

---

### 9. Scale demosu (vakit varsa)

```powershell
kubectl scale deployment/patent-app-deployment --replicas=1
kubectl get pods
```
**Bu ne yapiyor?** Pod sayisini 1'e dusurur. Bir pod silinir.

```powershell
kubectl scale deployment/patent-app-deployment --replicas=3
kubectl get pods
```
**Bu ne yapiyor?** 3'e cikarir, 2 yeni pod olusur.

```powershell
kubectl scale deployment/patent-app-deployment --replicas=2
```
> Sunumu 2 ile bitir.

**Hocaya soyle:** "K8s pod sayisini saniyeler icinde degistirebiliyor. Production'da yuk artinca otomatik olarak da scale edebilir."

---

## C) Hocadan Gelebilecek Sorular

**S: Bu projede DB var mi?**
Var — H2 in-memory database. Pod ayaga kalkinca seed data yukleniyor (3 author, 4 patent, 4 certification). PDF "no DB expected" demis ama biz daha gercekci olsun diye ekledik.

**S: 2 pod arasinda DB nasil paylasiliyor?**
Paylasilmiyor — her pod'un kendi H2 instance'i var. Yeni Author eklersen sadece o pod'da olur, diger pod'da olmaz. Production'da cozumu external DB (PostgreSQL pod'u veya managed DB) kullanmak. Demo amaciyla boyle birakildi.

**S: Pipeline neden 6 stage?**
PDF'in istedigi adimlar: 1) clone, 2) build jar, 3) docker build, 4) dockerhub login, 5) push, 6) k8s deploy.

**S: Image nereden cekiliyor?**
DockerHub'dan: `ukbey/patent-app:latest`. K8s deployment'in `imagePullPolicy: Always` olarak ayarli, her seferinde fresh ceker.

**S: Webhook nasil calisiyor?**
GitHub push'tan sonra HTTP POST gonderir → ngrok tuneli → lokal Jenkins. Jenkins'in "GitHub hook trigger" plugin'i bunu alir ve job'i tetikler.

**S: Author <-> Patent iliskisi nasil modellenmis?**
Many-to-Many iliski. Direkt `@ManyToMany` yerine **Certification** join entity'si kullandim cunku iliskide ekstra alanlar var (issueDate, durationYears). Certification, hem Author'a hem Patent'e `@ManyToOne` ile bagli.

**S: Frontend hangi teknoloji?**
Vanilla HTML + CSS + JavaScript. Spring Boot'un static resource ozelligi `index.html`'i `/` yolundan otomatik servis eder. Frontend `fetch()` ile REST API'lara istek atar.

**S: Site internette mi?**
Hayir, sadece lokalde. PDF zaten "local computer" ve "local Kubernetes cluster" diyor. Internete deploy gerekmiyor.

**S: REST endpoint'ler neler?**
- `GET /api/authors`, `POST /api/authors`, `DELETE /api/authors/{id}`
- `GET /api/patents`, `POST /api/patents`, `DELETE /api/patents/{id}`
- `GET /api/certifications`, `POST /api/certifications`, `DELETE /api/certifications/{id}`
- `GET /api/info` → JSON (pod hostname)
- `GET /api/hello` → text (load balancing testi icin)
- `/h2-console` → DB browser

---

## D) Bir Sey Ters Giderse Hizli Cozumler

| Sorun | Cozum |
|-------|-------|
| `minikube status` → Stopped | `minikube start --driver=docker` |
| Jenkins UI acilmiyor | Admin PowerShell'de: `Start-Service jenkins` |
| Webhook trigger olmuyor | ngrok URL degismistir → GitHub Settings/Hooks'tan guncelle |
| Pipeline "kubectl: command not found" | PATH'te kubectl var mi kontrol et, varsa Jenkins'i restart |
| Pipeline DockerHub'a push edemiyor | Jenkins'te `dockerhub-creds` credential var mi, ID birebir mi? |
| Pod ImagePullBackOff | DockerHub'da image public mi, deployment.yaml'da image adi dogru mu? |
| Tarayicida site acilmiyor | `minikube service patent-app-service` ile yeni tunel ac |
| 8080 port cakismasi | Jenkins zaten 8080'de — Spring Boot'u lokal calistirmaya kalkma, sadece Minikube uzerinden ac |

---

## E) Cok Hizli Ozet (Sunumdan 5 dakika once oku)

1. Docker Desktop ac → minikube start → kubectl apply
2. Jenkins servis kontrol → ngrok ac → GitHub webhook URL guncelle
3. Sunumda: cluster goster → Patent Manager ac → Jenkins goster → push yap → kendiliginden deploy → yenile → "LIVE DEMO BUILD" gor → load balancing icin F5 → bitir

**Mesaj:** "Hicbir manuel deploy yok. Sadece push. Gerisi otomatik."
