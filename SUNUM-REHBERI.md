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

### 1. Proje ozeti (sozlu)
"Spring Boot ile tek endpoint'li web app, GitHub'a push edilince Jenkins pipeline 6 stage'i otomatik calistirir, Docker image build edip DockerHub'a push eder, son adimda Minikube K8s cluster'a deploy eder. 2 pod'a scale edilmis."

### 2. Cluster ve servis ayakta
```powershell
minikube status
kubectl get nodes
kubectl get pods,svc
```
> 2 pod Running, NodePort service var.

### 3. Jenkins pipeline'i goster
Tarayici: <http://localhost:8080/job/patent-app-pipeline/>
- Stage View'da 6 yesil kutucuk goster (Clone, Build, Docker, Login, Push, Deploy).
- Bir build'e tikla -> Console Output -> hocaya stage'lerin loglarini goster.

### 4. **GitHub push trigger demosu** (en kritik)
Komut satirinda:
```powershell
cd C:\Users\ukbet\Desktop\DevopsProject4
"// sunum trigger test $(Get-Date)" | Add-Content src\main\java\com\example\patent\PatentController.java
git add .
git commit -m "Demo: trigger pipeline from push"
git push
```
Hemen tarayici -> Jenkins job sayfasini yenile.
- Yeni build (#N+1) **kendiliginden basladigini** goster.
- "Started by GitHub push by UKBey" mesajini goster.

### 5. DockerHub'da image
Tarayici: <https://hub.docker.com/r/ukbey/patent-app/tags>
- `latest` tag'inin yeni push'tan sonra guncellenmis "Last pushed" tarihini goster.

### 6. Endpoint cevabi (load balancing)
```powershell
kubectl run lb-test --rm -i --restart=Never --image=busybox -- sh -c "for i in 1 2 3 4 5 6 7 8 9 10; do wget -qO- http://patent-app-service:8080/; echo; done"
```
> Her cevapta "served by pod: ..." farkli pod hostname'i donmeli (load balancing kaniti).

### 7. Scale demo
```powershell
# Once 1 pod'a in
kubectl scale deployment/patent-app-deployment --replicas=1
kubectl get pods
# Sonra 2'ye geri cik
kubectl scale deployment/patent-app-deployment --replicas=2
kubectl get pods
```
> Pod'larin gercek zamanli olarak olusup yok oldugunu goster.

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
