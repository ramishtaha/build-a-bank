# Step 31 · Frontend pt.3 — Testing, Accessibility & i18n (MSW, Playwright, a11y, multi-currency)
### Phase F — Full-Stack Frontend 🔵 · Step 31 of 67

> *Step 30 connected our React app to real backend APIs. In Step 31, we master frontend quality. We'll introduce **MSW (Mock Service Worker)** for realistic network-level API mocking, write **Playwright** E2E specs that run in a real browser, implement **accessibility (a11y)** basics, and add **i18n** (internationalization) with multi-currency formatting.*

---

<a id="toc"></a>
## 🧭 The Six Movements of This Step

| | Movement | What happens |
|---|---|---|
| **A** | [🧭 Orient](#orient) | 30-second overview · skip-test · cheat card · why it matters · before you start |
| **B** | [🧠 Understand](#understand) | network-level mocking vs module mocking · the E2E testing pyramid · accessibility (a11y) basics · i18n architecture |
| **C** | [🛠️ Build](#build) | setup MSW · rewrite Vitest tests · a11y updates · react-i18next + Intl.NumberFormat · Playwright E2E spec |
| **D** | [🔬 Prove](#prove) | the Verification Log — MSW tests green, Axe clean, Playwright E2E passes |
| **E** | [🎓 Apply](#apply) | go deeper · interview prep · your-turn challenges |
| **F** | [🏆 Review](#review) | troubleshooting · resources · recap, flashcards & what's next |

---

<a id="orient"></a>

# A · 🧭 Orient

## 📋 This Step in 30 Seconds

| | |
|---|---|
| **Title** | Testing, Accessibility & i18n (MSW, Playwright, a11y, multi-currency) |
| **Step** | 31 of 67 · **Phase F — Full-Stack Frontend** 🔵 |
| **Effort** | ≈ 15 hours focused. Testing Library deepening, Playwright setup, i18n configuration. |
| **What you'll run this step** | **Node + npm** for the SPA. We'll run Vitest (component tests) and Playwright (E2E browser tests). |
| **Buildable artifact** | We replace `vi.mock` with **MSW** in `frontend/test/`. We add `react-i18next` for translations and `vitest-axe` for a11y checks. Finally, we add a Playwright E2E test suite in `frontend/e2e/`. |
| **Verification tier** | 🟠 **Standard**. `npm run build`, `npm run lint`, `npm run test` (with MSW), and `npx playwright test` all green. |
| **Depends on** | **[Step 30](../step-30/lesson.md)** (frontend data fetching and forms). |

By the end you'll **mock APIs at the network layer**, **verify accessibility rules automatically**, **format currency and translate strings**, and **write browser E2E tests**.

### ⏭️ Can You Skip This Step? (5-minute self-check)

If you can confidently do **all** of this, skim 🛠️ Build and jump to **[Step 32 — hardening & ship](../step-32/lesson.md)**.

- [ ] I can set up MSW and write handlers to mock REST endpoints for tests.
- [ ] I know how to test React components for accessibility violations using `axe-core`.
- [ ] I can configure `react-i18next` and format currencies safely using `Intl.NumberFormat`.
- [ ] I can write a Playwright test to simulate user flows across multiple pages.

> [!TIP]
> Not 100%? Stay. "How do you test your frontend?", "How do you handle multi-language support?", and "How do you ensure accessibility?" are standard frontend interview questions.

## 📇 Cheat Card

> **What this step delivers (one sentence):** A robustly tested React application with network-level mocking, localized currency formatting, automated accessibility checks, and full browser end-to-end tests.

**Key commands** (in `frontend/`):

```bash
npm test                  # Vitest + MSW
npx playwright test       # Playwright E2E tests
npx playwright show-report # View E2E results
```

**The headline — MSW and Playwright:**

```javascript
// MSW intercepts requests at the network layer:
http.get('*/api/accounts/:id', () => HttpResponse.json({ balance: 100 }))

// Playwright drives a real browser:
await page.getByRole('textbox', { name: /username/i }).fill('alice');
await expect(page.getByText('Balance: 100')).toBeVisible();
```

## 🎯 Why This Matters

Fragile tests that mock internal modules (`vi.mock('../api')`) break when implementation details change. **MSW** intercepts real HTTP requests, so your code doesn't know it's being tested. Accessibility (**a11y**) isn't a nice-to-have; it's a legal requirement and good business. **Playwright** ensures that the entire stack (or at least the whole frontend against mocks) works together as the user experiences it.

## ✅ What You'll Be Able to Do

- Replace brittle module mocks with robust MSW network mocks.
- Catch accessibility bugs early with `vitest-axe` and ARIA attributes.
- Format money properly for different locales using `Intl.NumberFormat`.
- Prove your critical paths work in a real browser with Playwright.

## 🧰 Before You Start

- **Prereqs:** Node 22 + npm; the Step-30 `frontend/` builds.
- **Depends on:** Step **30**.

---

<a id="understand"></a>

# B · 🧠 Understand

## 🧠 The Big Idea — Network-Level Mocking vs Module Mocking

In Step 30, we used `vi.mock('../api/client')` to simulate API responses. This is **module mocking**. It works, but it's brittle. If we refactor *how* we make requests (e.g., swapping `fetch` for `axios`, or changing internal client structure), the test breaks even if the app still works.

**Mock Service Worker (MSW)** uses a different approach: **network-level mocking**. It uses a Service Worker (in the browser) or Node's HTTP interceptors (in Vitest/Jest) to intercept actual outbound requests. Your app makes real `fetch` calls, and MSW intercepts them and returns mock responses. The app thinks it's talking to a real backend. This gives you confidence that your request formatting, headers, and parsing all work correctly.

## 🧩 Pattern Spotlight — The Testing Pyramid in the Frontend

- **Unit/Component Tests (Testing Library + Vitest):** Fast, isolated, mock the network (MSW). Tests specific states, interactions, and edge cases.
- **End-to-End Tests (Playwright):** Slower, runs in a real browser (Chromium, Firefox, WebKit). Tests the full user journey (Login -> View Dashboard -> Transfer Money).

## 🌱 Under the Hood: Accessibility (a11y) & ARIA

Screen readers and assistive technologies rely on the **Accessibility Tree**, a stripped-down version of the DOM. When you use semantic HTML (`<button>`, `<nav>`), the browser builds this tree automatically. When building complex UI, you sometimes need **ARIA** (Accessible Rich Internet Applications) attributes like `aria-live="polite"` (to announce dynamic updates, like a toast notification) or `aria-invalid` (to signal form errors). We'll use `vitest-axe` to automatically scan our components for WCAG violations.

## 🕰️ Then vs. Now — i18n and Currency

- **Then:** Developers wrote custom utility functions to parse and format currencies, dealing with commas, dots, and currency symbols manually. This was a nightmare of edge cases.
- **Now:** `Intl.NumberFormat` is built into JavaScript. It handles localization, currency symbols, and grouping perfectly. `react-i18next` handles translating static strings, while `Intl` handles the numbers.

---

<a id="build"></a>

# C · 🛠️ Let's Build It — Step by Step

## 📦 Your Starting Point

`step-31-start == step-30-end`: the React app fetches data via TanStack Query and uses `vi.mock` in tests.
