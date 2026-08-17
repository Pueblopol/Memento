# 📝 Memento

Memento è un'app Android moderna, veloce e completamente offline per prendere appunti, organizzare le idee e gestire le tue attività quotidiane. Sviluppata interamente con **Kotlin** e **Jetpack Compose**, punta a offrire un'esperienza utente fluida, silenziosa e priva di distrazioni.

## ✨ Funzionalità Principali

*   **Organizzazione Perfetta:** Crea cartelle personalizzate e raggruppa le tue note con facilità.
*   **Archivio:** Archivia le note che non ti servono più per mantenere la Home pulita, senza cancellarle definitivamente.
*   **Priorità Visiva:** Assegna livelli di priorità (Alta, Media, Bassa) con indicatori a colori chiari e minimalisti.
*   **Fissate in Alto (Pin):** Tieni le note più importanti sempre in cima alla lista con la funzione *Pin*.
*   **Gestione a Scorrimento (Swipe):** Archivia, ripristina o elimina le note con un semplice e fluido gesto di *swipe*.
*   **Formattazione Intelligente (Rich Text):** La descrizione supporta il grassetto (`**testo**`), elenchi puntati, numerati auto-continuanti e **Checklist Interattive** (`☐`/`☑`) che puoi spuntare direttamente dalla lettura, senza dover entrare in modalità modifica.
*   **Drag & Drop:** Riordina le tue cartelle semplicemente trascinandole nella posizione che preferisci.
*   **Visualizzazione Personalizzabile:** Passa dinamicamente dalla visualizzazione a Lista (List View) a quella a Griglia (Grid View) con un solo tap.
*   **Dark Mode Adattiva:** Supporto completo per il tema scuro o chiaro in base alle preferenze del tuo sistema, con un'interfaccia curata nei minimi dettagli.
*   **Condivisione Veloce:** Condividi rapidamente il testo delle tue note su altre app (es. WhatsApp, Email).

## 🛠️ Tecnologie Utilizzate

*   **Linguaggio:** [Kotlin](https://kotlinlang.org/)
*   **Interfaccia Grafica:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
*   **Database:** [Room Database](https://developer.android.com/training/data-storage/room) per un salvataggio dei dati sicuro, reattivo e `offline-first`.
*   **Architettura:** MVVM (Model-View-ViewModel) con l'utilizzo di *Coroutines* e *StateFlow* per una reattività dell'interfaccia istantanea.

## 🚀 Come avviare il progetto

1. Clona il repository:
   ```bash
   git clone https://github.com/pol/Memento.git
   ```
2. Apri il progetto con **Android Studio** (Koala o successivi raccomandati).
3. Attendi il termine del sync di Gradle.
4. Premi `Run` (Shift+F10) per lanciare l'app su un emulatore o sul tuo dispositivo fisico.

## 🤝 Contribuire

Sentiti libero di aprire una *Issue* o inviare una *Pull Request* se hai idee, suggerimenti o bug fix per migliorare Memento!

---
*Progettato per essere semplice, pensato per essere utile.*
