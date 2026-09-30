const base = process.env.API_URL ?? 'http://localhost:8080';
const total = Number(process.env.USUARIOS ?? 20);

for (let i = 1; i <= total; i++) {
  const res = await fetch(`${base}/v1/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: `perf${i}@test.com`, user: `perf${i}`, password: 'Senha@Forte123' }),
  });
  const estado = res.status === 201 ? 'criado' : res.status === 409 ? 'já existia' : `erro ${res.status}`;
  console.log(`perf${i}@test.com: ${estado}`);
}
