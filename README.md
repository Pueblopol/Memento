# 📝 Memento

Memento è un'app Android moderna, veloce e progettata per chi desidera il controllo totale sui propri dati. Sviluppata per offrire un'esperienza di scrittura fluida e priva di distrazioni, unisce l'immediatezza di un'app per appunti alla potenza della **sincronizzazione Git-based** in formato Markdown.

Nessun server proprietario, nessun abbonamento cloud obbligatorio: i tuoi appunti sono tuoi, sempre accessibili offline e sincronizzati privatamente dove decidi tu.

## ✨ Perché scegliere Memento?

*   **☁️ Sincronizzazione Git "Invisibile" (Offline-first):** Memento salva e organizza i dati localmente per offrirti velocità e disponibilità istantanea, anche in aereo. Sotto al cofano, converte magicamente ogni nota in un file Markdown puro e lo sincronizza in background su un tuo repository Git privato. Niente conflitti, niente perdite di dati: fa tutto da sola quando torni online.
*   **☑️ Editor Markdown "Vivo":** Non solo supporta la classica formattazione (grassetto, elenchi puntati e numerati che si completano da soli andando a capo), ma le tue checklist (`☐`/`☑`) sono interattive! Puoi spuntarle direttamente leggendo la nota, senza neppure entrare in modalità modifica. I link web vengono estratti automaticamente in comodi "chip" pronti da cliccare.
*   **🗂️ Organizzazione Sartoriale:** Crea cartelle, fissa in alto (Pin) le urgenze e assegna priorità a colori (Alta, Media, Bassa). Che tu voglia ordinare per priorità o usare il **Drag & Drop** libero per disporre cartelle e note esattamente dove vuoi tu, Memento si adatta al tuo workflow.
*   **📱 Layout Adattivo:** Preferisci la visione d'insieme di una Griglia (Grid View) o l'ordine rigoroso di una Lista (List View)? Passa da una all'altra con un tap. L'app adatta le gesture (come lo Swipe-to-archive o il Long-Press) per evitare cancellazioni accidentali in base alla visualizzazione che scegli.
*   **🌙 Cura dei Dettagli:** Dark Mode dinamica nativa, archivio per nascondere le note completate senza perderle e un'interfaccia pulita in Material Design 3.

## 🛠️ Tecnologie Utilizzate

Dietro alla sua semplicità, Memento nasconde un'architettura robusta:
*   **Linguaggio:** [Kotlin](https://kotlinlang.org/)
*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) per un'interfaccia reattiva e moderna.
*   **Storage Locale:** [Room Database](https://developer.android.com/training/data-storage/room) per garantire la filosofia offline-first.
*   **Motore di Sincronizzazione:** [JGit](https://www.eclipse.org/jgit/) integrato con Coroutines e un sistema a Code (Mutex) per gestire convezione Markdown/Database in modo thread-safe e risolvere conflitti git in modo trasparente all'utente.

## 🚀 Come provare Memento

1. Clona il repository sul tuo computer:
   ```bash
   git clone https://github.com/Pueblopol/Memento.git
   ```
2. Apri la cartella con **Android Studio** (versione Koala o superiore).
3. Attendi il termine della sincronizzazione di Gradle.
4. Premi `Run` (Shift+F10) per lanciare l'app su un emulatore o sul tuo telefono Android.

## 🤝 Contribuire

Sentiti libero di aprire una *Issue* o inviare una *Pull Request* se hai idee, suggerimenti o bug fix per migliorare Memento!

---
*Progettato per essere semplice, pensato per essere utile.*
