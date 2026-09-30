const base = process.env.API_URL ?? 'http://localhost:8080';
const email = process.env.PERF_EMAIL ?? 'perf1@test.com';
const password = process.env.PERF_PASSWORD ?? 'Senha@Forte123';
const seconds = Number(process.env.DURATION ?? 15);
const concurrency = Number(process.env.CONNECTIONS ?? 16);
const warmup = process.env.WARMUP !== '0';

const iso = (d) => d.toISOString().slice(0, 10);
const today = new Date();
const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);
const thirtyDaysAgo = new Date(today.getTime() - 30 * 86400000);

const login = await fetch(`${base}/v1/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password }),
});
const { token } = await login.json();
const auth = { Authorization: `Bearer ${token}` };
const json = { ...auth, 'Content-Type': 'application/json' };

const scenarios = [
  { name: 'Listar transações (página 1)', path: '/transaction?size=10', headers: auth },
  { name: 'Listar transações (página 5000, offset 50 mil)', path: '/transaction?size=10&page=5000', headers: auth },
  { name: 'Listar com filtro (30 dias + categoria)', path: `/transaction?size=10&category=FOOD&startDate=${iso(thirtyDaysAgo)}&endDate=${iso(today)}`, headers: auth },
  { name: 'Resumo de tudo (100 mil linhas)', path: '/transaction/summary', headers: auth },
  { name: 'Resumo do mês', path: `/transaction/summary?startDate=${iso(monthStart)}&endDate=${iso(today)}`, headers: auth },
  { name: 'Total por categoria (tudo)', path: '/transaction/summary/by-category', headers: auth },
  {
    name: 'Criar transação',
    path: '/transaction',
    method: 'POST',
    headers: json,
    body: JSON.stringify({ description: 'Carga', amount: 12.5, type: 'EXPENSES', category: 'FOOD' }),
  },
  {
    name: 'Login (bcrypt)',
    path: '/v1/auth/login',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
    workers: 4,
  },
];

const percentile = (sorted, p) => sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * p))];

async function run(scenario, durationSeconds) {
  const latencies = [];
  let errors = 0;
  const endAt = performance.now() + durationSeconds * 1000;
  const startedAt = performance.now();

  async function worker() {
    while (performance.now() < endAt) {
      const t0 = performance.now();
      try {
        const res = await fetch(`${base}${scenario.path}`, {
          method: scenario.method ?? 'GET',
          headers: scenario.headers,
          body: scenario.body,
        });
        await res.arrayBuffer();
        if (!res.ok) errors++;
      } catch {
        errors++;
      }
      latencies.push(performance.now() - t0);
    }
  }

  await Promise.all(Array.from({ length: scenario.workers ?? concurrency }, worker));
  const elapsed = (performance.now() - startedAt) / 1000;
  latencies.sort((a, b) => a - b);
  return {
    cenario: scenario.name,
    requisicoes: latencies.length,
    reqPorSegundo: Math.round(latencies.length / elapsed),
    p50: +percentile(latencies, 0.5).toFixed(1),
    p95: +percentile(latencies, 0.95).toFixed(1),
    p99: +percentile(latencies, 0.99).toFixed(1),
    max: +latencies[latencies.length - 1].toFixed(1),
    erros: errors,
  };
}

if (warmup) {
  for (const s of scenarios.slice(0, 6)) await run(s, 4);
}

const rows = [];
for (const s of scenarios) {
  const r = await run(s, seconds);
  rows.push(r);
  console.error(`${r.cenario}: ${r.reqPorSegundo} req/s, p50 ${r.p50} ms, p95 ${r.p95} ms, p99 ${r.p99} ms, erros ${r.erros}`);
}
console.log(JSON.stringify(rows, null, 2));
