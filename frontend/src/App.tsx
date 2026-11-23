import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import i18n from "./i18n";

export default function App() {
  const { t } = useTranslation();
  const [q, setQ] = useState("spring boot");
  const [results, setResults] = useState<any[]>([]);
  const [selected, setSelected] = useState<string>("spring-projects/spring-boot");
  const [health, setHealth] = useState<any>(null);
  const [watchlist, setWatchlist] = useState<any[]>([]);
  const [error, setError] = useState("");

  async function search() {
    setError("");
    const res = await fetch(`/api/repositories/search?q=${encodeURIComponent(q)}`);
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();
    setResults(data.items || []);
  }

  async function analyze(fullName: string) {
    setSelected(fullName);
    setError("");
    const [owner, repo] = fullName.split("/");
    const res = await fetch(`/api/repositories/${owner}/${repo}/health`);
    if (!res.ok) throw new Error(await res.text());
    setHealth(await res.json());
  }

  async function loadWatch() {
    const res = await fetch("/api/watchlist");
    setWatchlist(await res.json());
  }

  async function addWatch() {
    await fetch("/api/watchlist", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ fullName: selected, notes: "tracked from UI" })
    });
    await loadWatch();
  }

  useEffect(() => {
    search().catch((e) => setError(String(e)));
    loadWatch().catch(() => undefined);
  }, []);

  const chartData = useMemo(
    () => (health?.dimensions || []).map((d: any) => ({ name: d.id, score: d.score })),
    [health]
  );

  return (
    <div className="app">
      <div className="row" style={{ justifyContent: "space-between" }}>
        <div>
          <div className="brand">{t("appName")}</div>
          <div className="tagline">{t("tagline")}</div>
        </div>
        <select
          value={i18n.language}
          onChange={(e) => {
            i18n.changeLanguage(e.target.value);
            localStorage.setItem("or.locale", e.target.value);
          }}
        >
          <option value="en">English</option>
          <option value="zh-CN">简体中文</option>
        </select>
      </div>

      <section className="panel">
        <div className="row">
          <input style={{ flex: 1 }} value={q} onChange={(e) => setQ(e.target.value)} placeholder={t("search")} />
          <button onClick={() => search().catch((e) => setError(String(e)))}>{t("search")}</button>
        </div>
        {error && <p className="muted">{error}</p>}
        <ul className="list" style={{ marginTop: 12 }}>
          {results.map((r) => (
            <li key={r.full_name} onClick={() => analyze(r.full_name).catch((e) => setError(String(e)))}>
              <strong>{r.full_name}</strong>
              <div className="muted">{r.description}</div>
              <div className="muted">★ {r.stargazers_count} · forks {r.forks_count} · {r.language}</div>
            </li>
          ))}
        </ul>
      </section>

      <section className="panel">
        <div className="row">
          <h2 style={{ margin: 0 }}>{t("analyze")}: {selected}</h2>
          <button onClick={addWatch}>{t("addWatch")}</button>
        </div>
        {health && (
          <>
            <p className="muted">
              overall {health.overallScore} · model {health.modelVersion} · {health.analyzedAt}
            </p>
            <div style={{ width: "100%", height: 260 }}>
              <ResponsiveContainer>
                <BarChart data={chartData}>
                  <XAxis dataKey="name" stroke="#9aa8c7" />
                  <YAxis stroke="#9aa8c7" domain={[0, 100]} />
                  <Tooltip />
                  <Bar dataKey="score" fill="#6ea8ff" />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <pre>{JSON.stringify(health, null, 2)}</pre>
          </>
        )}
      </section>

      <section className="panel">
        <h2>{t("watchlist")}</h2>
        <pre>{JSON.stringify(watchlist, null, 2)}</pre>
      </section>
    </div>
  );
}
