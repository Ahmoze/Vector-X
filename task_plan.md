# Vector App Nadogradnje - Task Plan

## 1. Lažiranje "User-Agent" zaglavlja (Bypass WAF)
- [x] Modifikacija `App.java` fajla.
- [x] Dodavanje Interceptor-a u `OkHttpClient` koji će ubaciti `User-Agent` sličan pravom pretraživaču (kako bi se zaobišle Cloudflare i ostale WAF blokade).

## 2. Bolje rukovanje greškama u interfejsu (UI)
- [x] Ažuriranje `RepoFragment.java` i `RepoItemFragment.java`.
- [x] Rešavanje problema beskonačnog "Loading..." ekrana kada `RepoLoader` naiđe na mrežnu grešku (`onThrowable`).
- [x] Prikazivanje odgovarajuće poruke (Snackbar/Toast) korisniku.

## 3. Podrška za prilagođene repozitorijume (Custom Repos)
- [x] Dodavanje opcije u Settings fragmentu/XML-u za promenu URL-a repozitorijuma.
- [x] Čitanje prilagođenog URL-a iz SharedPreferences-a unutar `RepoLoader.java` fajla (umesto hardkodovanih adresa).

## 4. Prelazak na "Material You" (Material 3) (Opciono/U zavisnosti od potrebe)
- [ ] Provera postojećih tema i ažuriranje na `Theme.Material3.*` ukoliko je izvodljivo, kako bi UI podržavao Dynamic Colors.

## 5. Optimizacija u vidu Lazy Loading-a (Opciono)
- [ ] Prepravljanje logike za preuzimanje paginiranih stranica samo po potrebi umesto skidanja svih stranica unapred.
