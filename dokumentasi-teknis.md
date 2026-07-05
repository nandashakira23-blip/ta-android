# Fleur Atelier - Android Presensi

## Sumber Acuan Aktual

- Backend utama: `app.js`, `routes/admin.js`, `routes/api.js`, `swagger.js`.
- Database aktual: `migrations/database.js` dan migrasi runtime di `app.js`.
- Android aktual: `Android App/app/src/main/java/com/fleur/attendance/data/api/ApiService.kt` dan model di folder `data/model`.
- Web admin/manager aktual: `views/admin/*` dan `views/partials/sidebar.ejs`.

Dokumen ini diperbarui pada 2026-05-07 agar mengikuti sistem yang sedang berjalan, bukan rancangan lama.

## Ringkasan

Aplikasi Android dipakai karyawan untuk aktivasi akun, login, presensi masuk, istirahat, presensi keluar, pengajuan Absensi cuti/izin/sakit, approval pengganti, riwayat presensi, profil, ganti PIN, dan enroll ulang wajah.

## Android App

- Aktivasi akun dengan NIK, PIN, email OTP, dan face enrollment.
- Login memakai NIK dan PIN, lalu token JWT disimpan untuk request berikutnya.
- Clock in dan clock out memakai foto, GPS, validasi radius, dan face matching.
- Istirahat memakai endpoint start/end; detail sesi tersimpan di `presensi.data_istirahat`.
- Pengajuan Absensi untuk `cuti`, `izin`, dan `sakit` mendukung `full_day`, `half_day`, dan `hourly`.
- Jika ada `id_pengganti`, status awal menjadi `menunggu_pengganti`; setelah disetujui pengganti, status menjadi `menunggu_manager`.
- Karyawan yang dipilih sebagai pengganti melihat daftar request di `/api/replacement-requests` dan dapat approve/reject.
- Riwayat presensi menampilkan menit kerja, terlambat, pulang awal, lembur, istirahat, dan absensi yang disetujui.

## Struktur Kode Penting

| Lokasi | Fungsi |
| --- | --- |
| `data/api/ApiService.kt` | Definisi endpoint Retrofit yang dipakai Android. |
| `data/api/TokenManager.kt` | Inject token JWT dan refresh token. |
| `data/repository/AttendanceRepository.kt` | Presensi masuk/keluar, istirahat, status, riwayat, ringkasan. |
| `data/repository/LeaveRepository.kt` | Pengajuan Absensi dan approval pengganti. |
| `ui/attendance/ClockInActivity.kt` | Presensi masuk. |
| `ui/attendance/ClockOutActivity.kt` | Presensi keluar. |
| `ui/attendance/LeaveRequestActivity.kt` | Form Absensi cuti/izin/sakit. |
| `ui/attendance/AttendanceHistoryActivity.kt` | Riwayat presensi dan status. |
| `utils/SessionManager.kt` | Penyimpanan data session karyawan di perangkat. |

## Endpoint Aktual

| Area | Endpoint | Fungsi |
| --- | --- | --- |
| System | `GET /api/health` | Health check service dan database. |
| Auth Android | `POST /api/auth/check-nik`, `/login`, `/activate`, `/refresh`, `/logout` | Validasi NIK, aktivasi, login PIN, refresh token, logout. |
| Email | `POST /api/auth/request-email-otp`, `/verify-email-otp`, `/request-email-verification` | Verifikasi email karyawan. |
| Face | `GET /api/employee/face-reference`, `/api/face/status`, `POST /api/face/re-enroll` | Referensi wajah dan enroll ulang. |
| Presensi | `POST /api/attendance/checkin`, `/checkout`, `/validate-face` | Presensi masuk/keluar dengan lokasi, foto, dan face matching. |
| Istirahat | `POST /api/attendance/break/start`, `/break/end` | Mulai dan selesai istirahat, disimpan ke `presensi.data_istirahat`. |
| Riwayat Presensi | `GET /api/attendance/status/:id_karyawan`, `/today`, `/history`, `/summary` | Status hari ini, riwayat, dan ringkasan presensi. |
| Absensi Android | `GET/POST /api/leave-requests` | Pengajuan dan daftar cuti/izin/sakit pada tabel `absensi`. |
| Approval Pengganti | `GET /api/replacement-requests`, `POST /api/replacement-requests/:id/approve`, `/reject` | Karyawan pengganti approve/reject sebelum manager. |
| Jadwal | `GET /api/schedule/today/:id_karyawan` | Jadwal kerja karyawan hari ini. |
| Lokasi | `GET /api/settings/office-location`, `POST /api/validation/location` | Ambil dan validasi lokasi kantor. |
| Admin Web | `/admin/*` | Dashboard, master data, laporan, pengaturan, akun manager, dan Manajemen Absensi. |

Dokumentasi Swagger berjalan di `/api-docs` dari `swagger.js` dan komentar OpenAPI di `routes/api.js`.

## Base URL

- Emulator Android: `http://10.0.2.2:3000/`.
- Device fisik: gunakan IP komputer backend di jaringan yang sama.
- Production: set URL API production di `ApiConfig.kt` sesuai deployment.

## Catatan Istilah

Nama class Android masih menggunakan `Leave*` pada beberapa file agar kompatibel dengan implementasi sebelumnya. Secara bisnis dan database, fitur tersebut adalah `Absensi` dan tersimpan pada tabel `absensi`.
