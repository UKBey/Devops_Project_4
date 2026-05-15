# SWE304 Project 4 — Sunum Rehberi

## A) Sunumdan ONCE (bilgisayari acmissan ya da yeniden basladiysan)

> Sira onemli — bir oncekini bitirmeden digerine gecme.

### 1. Docker Desktop'i baslat
Baslat menusu -> **Docker Desktop**. Sag alttaki tray icon yesil olana kadar bekle (~30 sn).

```powershell
docker version
```
> Server bilgisi gelmeli. Gelmezse Docker Desktop tam baslamamis demektir.

### 2. Minikube cluster'i baslat
```powershell
minikube start --driver=docker
minikube status
kubectl get nodes
```
> `host: Running` ve node `Ready` olmali. Ilk acmiyorsan 30-60 sn, eski cluster ayagaya kalkar.

### 3. K8s deployment ve service'i yeniden uygula (opsiyonel)
Onceden uygulanmis ama gunes guvenligi olsun:
```powershell
cd C:\Users\ukbet\Desktop\DevopsProject4
kubectl apply -f k8s\deployment.yaml
kubectl apply -f k8s\service.yaml
kubectl scale deployment/patent-app-deployment --replicas=2
kubectl get pods
```
> 2 pod Running olmali.

### 4. Jenkins servis kontrolu
```powershell
Get-Service jenkins
```
> Status `Running` olmali. Degilse: `Start-Service jenkins` (admin PowerShell'de).

Tarayicidan ac: <http://localhost:8080> -> admin / kurulumda belirledigin parola ile gir.

### 5. ngrok tunelini ac (webhook icin)
```powershell
ngrok http 8080
```
> Acilan pencerede `Forwarding` satirinda **yeni public URL** gozukur:
> `https://XXXX-XXXX.ngrok-free.dev`
> Bu URL **her ngrok baslattiginda DEGISIR** (ucretsiz plan).

### 6. GitHub webhook URL'sini guncelle
1. <https://github.com/UKBey/Devops_Project_4/settings/hooks>
2. Mevcut webhook'a tikla.
3. **Payload URL** alanini guncelle:
   `https://YENI-URL.ngrok-free.dev/github-webhook/` (sondaki `/` sart!)
4. **Update webhook** -> yesil onay gelmeli.

> Webhook URL'sini guncellemezsen push trigger calismaz.

### 7. Son hazirlik kontrolu
```powershell
minikube status
Get-Service jenkins
kubectl get pods
docker version --format '{{.Server.Version}}'
```
Hepsi calisiyorsa **HAZIR**.

---

## B) SUNUM SIRASI (hocaya gosterilecek adimlar)

> **Hedef:** PDF'in a-e maddelerini tek bir akista kanitlamak.
> En kritik bolum: **Adim 5** — push yaptiginda hem Jenkins pipeline tetiklenir, hem yeni frontend canliya cikar (a/b/c/d/e maddelerini ayni anda kapsar).

### 1. Proje ozeti (sozlu)
"Spring Boot web uygulamasi, modern bir frontend'i ve `/api/info` JSON endpoint'i var. GitHub'a push yapinca Jenkins pipeline 6 stage'i otomatik calistirir: clone, jar build, docker build, dockerhub login, push, K8s deploy. Minikube cluster'da 2 replica halinde calisiyor — frontend canli olarak hangi pod'un cevapladigini ve load balancing dagilimini gosteriyor."

### 2. Cluster ve servis ayakta
```powershell
minikube status
kubectl get nodes
kubectl get pods,svc
```
> 2 pod Running, NodePort service var.

### 3. Mevcut frontend'i ac (push'tan ONCE)
```powershell
minikube service patent-app-service
```
> Tarayici otomatik acilir. Hocaya goster:
> - Buyuk yesil kutuda **canli pod hostname** (1 sn'de bir auto-refresh)
> - Pod degisince yesil kutu parlar, "Pod Degisimi" sayaci artar
> - Load Balancing Dagilimi tablosu iki pod'un dagilimini gosterir
>
> **Tarayici sekmesini acik birak**, sunum boyunca burayi yenileyecegiz.

### 4. Jenkins pipeline'i goster (push'tan ONCE)
Tarayici: <http://localhost:8080/job/patent-app-pipeline/>
- Stage View'da onceki build'in 6 yesil kutucugunu goster (Clone, Build, Docker, Login, Push, Deploy).
- Son build numarasini **aklinda tut** (ornek: #5). Birazdan push yaptiginda #6 baslayacak.
- Bir build'e tikla -> Console Output -> stage loglarini hocaya goster.

### 5. **PUSH TRIGGER + CANLI DEPLOY DEMOSU** (en kritik adim — c/d/e maddelerinin kaniti)

**Amac:** Hocaya "kod degisikligimin GitHub'a push edilmesi -> Jenkins'in otomatik tetiklenmesi -> DockerHub'a yeni image push'lanmasi -> K8s'in otomatik rollout yapmasi -> frontend'de canli yansima" zincirini ucu uca gostermek.

**Gorsel olarak farkedilecek ufak bir degisiklik yap.** Ornek: `index.html`'deki baslik altindaki aciklamaya bir kelime ekle ya da yeni badge koy.

```powershell
cd C:\Users\ukbet\Desktop\DevopsProject4
# Ornek: header aciklamasinin sonuna "(LIVE DEMO)" ekle
(Get-Content src\main\resources\static\index.html) -replace 'Spring Boot \+ Docker \+ Kubernetes \+ Jenkins CI/CD', 'Spring Boot + Docker + Kubernetes + Jenkins CI/CD - LIVE DEMO' | Set-Content src\main\resources\static\index.html

git add .
git commit -m "Demo: trigger pipeline from push"
git push
```

**Sonra hizlica gorsel takip:**
1. Jenkins job sayfasini yenile -> **#6 build kendiliginden basladi**, "Started by GitHub push by UKBey" mesajini goster.
2. Stage View'da yesil kutucuklarin sirayla dolmasini izleyin (~2-3 dk).
3. Pipeline bitince **adim 3'teki tarayici sekmesini yenile** -> baslik altinda "LIVE DEMO" yazisi gozukur.
4. "Iste pipeline gercekten kodumu DockerHub'a push'ladi, K8s yeni image'i pull'ladi ve canliya cikardi — hicbir manuel adim yok."

> Bu tek adim PDF'in **(c) CI-CD pipeline**, **(d) Run on K8s**, **(e) Show as expected** maddelerini ucunu birden ispatlar.

### 6. DockerHub'da image (push'un dogrulanmasi)
Tarayici: <https://hub.docker.com/r/ukbey/patent-app/tags>
- `latest` tag'inin "Last pushed" tarihinin **az once** oldugunu goster.

### 7. Load balancing demosu (frontend uzerinden)
- Adim 3'teki tarayici sekmesine geri don.
- "Sayaclari Sifirla" butonuna bas.
- 30-40 sn auto-refresh acik birak.
- "Load Balancing Dagilimi" tablosunda **iki pod'un yaklasik 50/50 dagildigini** hocaya goster.
- "Pod Degisimi" sayacinin artarak ilerledigini goster.

### 8. Cluster icinden ek kanit (busybox)
```powershell
kubectl run lb-test --rm -i --restart=Never --image=busybox -- sh -c "for i in 1 2 3 4 5 6 7 8 9 10; do wget -qO- http://patent-app-service:8080/api/hello; echo; done"
```
> Her cevapta "served by pod: ..." farkli pod hostname'i donmeli (cluster ICINDEN de load balancing calisiyor).

### 9. Scale demo (eklenti — vakit varsa)
```powershell
# Once 1 pod'a in
kubectl scale deployment/patent-app-deployment --replicas=1
kubectl get pods
# Frontend tarayici sekmesinde sayaclari sifirla -> tek pod'a dustugu gozukur
# Sonra 2'ye geri cik
kubectl scale deployment/patent-app-deployment --replicas=2
kubectl get pods
```
> Pod'larin gercek zamanli olusup yok oldugunu hem terminalde hem frontend tablosunda goster.

---

## C) Sorulara hazirlikli ol

**S: Pipeline neden 6 stage?**
PDF'in istedigi: 1) clone, 2) build jar, 3) docker build, 4) dockerhub login, 5) push, 6) k8s deploy.

**S: Image nereden cekiliyor?**
DockerHub'dan (`ukbey/patent-app:latest`). K8s deployment `imagePullPolicy: Always` ile her seferinde fresh ceker.

**S: Webhook nasil calisiyor?**
GitHub push'tan sonra HTTP POST atiyor `ngrok-free.dev/github-webhook/` adresine. ngrok bunu localhost:8080'deki Jenkins'e tunelliyor. Jenkins "GitHub hook trigger" plugin'i ile job'i tetikliyor.

**S: 2 pod arasinda yuk dagilimi nasil?**
K8s Service (kube-proxy) round-robin yapiyor. busybox testi 10 istegi 2 pod'a dagitti.

**S: Local'de DB var mi?**
Yok. PDF "no DB expected" diyor, sadece tek endpoint olmasi yeterli.

**S: Frontend nasil servis ediliyor?**
Spring Boot, `src/main/resources/static/index.html` dosyasini otomatik olarak `/` yolundan servis eder (Spring Boot static resource handler). Frontend, `/api/info` JSON endpoint'ine her saniye AJAX istegi atip canli pod hostname'ini cekiyor. Boylece her istek farkli pod'a gidebildigi icin load balancing canli olarak gorulebiliyor.

**S: Site nasil aciliyor (web'de mi)?**
Web'de degil, lokalde. `minikube service patent-app-service` komutu Minikube cluster'daki NodePort service icin gecici bir tunel acar ve tarayicida acar. PDF zaten "local computer" ve "local Kubernetes cluster" istiyor.

---

## D) Sorun cikarsa hizli cozumler

| Belirti | Cozum |
|---|---|
| `minikube status` -> Stopped | `minikube start --driver=docker` |
| Jenkins UI acilmiyor | `Start-Service jenkins` (admin PS) |
| Webhook trigger olmuyor | ngrok URL degismistir -> GitHub Settings/Hooks'tan guncelle |
| Pipeline "kubectl: command not found" | Machine PATH'te kubectl var mi kontrol et, yoksa restart Jenkins |
| Pipeline DockerHub'a push edemiyor | Jenkins'te `dockerhub-creds` credential var mi, ID birebir mi? |
| Pod ImagePullBackOff | DockerHub'da image public mi, deployment.yaml'da image adi dogru mu (`ukbey/patent-app:latest`)? |
