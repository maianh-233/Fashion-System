import { useState } from "react";

import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Tooltip,
  Filler,
} from "chart.js";

import { Line } from "react-chartjs-2";

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Tooltip,
  Filler
);

export default function RevenueChart() {
  const [chartMode, setChartMode] = useState("weekly");

  const weeklyData = {
    labels: ["T2", "T3", "T4", "T5", "T6", "T7", "CN"],
    values: [42, 58, 45, 67, 52, 71, 68],
  };

  const yearlyData = {
    labels: ["2022", "2023", "2024", "2025", "2026"],
    values: [4.2, 5.6, 6.1, 7.4, 8.2],
  };

  const activeData = chartMode === "weekly" ? weeklyData : yearlyData;

  const data = {
    labels: activeData.labels,
    datasets: [
      {
        data: activeData.values,
        borderColor: "#fbbf24",
        backgroundColor: "rgba(251,191,36,0.1)",
        tension: 0.35,
        fill: true,
      },
    ],
  };

  const options = {
    responsive: true,
    plugins: {
      legend: {
        display: false,
      },
    },
    scales: {
      y: {
        ticks: {
          color: "#a1a1aa",
        },
        grid: {
          color: "rgba(161,161,170,0.15)",
        },
      },
      x: {
        ticks: {
          color: "#a1a1aa",
        },
        grid: {
          color: "rgba(161,161,170,0.08)",
        },
      },
    },
  };

  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <div className="flex flex-wrap gap-3 items-center justify-between mb-5">
        <h3 className="font-semibold">
          {chartMode === "weekly"
            ? "Doanh thu 7 ngày gần nhất"
            : "Thống kê doanh thu theo năm"}
        </h3>

        <div className="bg-zinc-800 rounded-xl p-1 flex gap-1">
          <button
            onClick={() => setChartMode("weekly")}
            className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
              chartMode === "weekly"
                ? "bg-amber-400 text-zinc-900 font-medium"
                : "text-zinc-300 hover:bg-zinc-700"
            }`}
          >
            7 ngày
          </button>

          <button
            onClick={() => setChartMode("yearly")}
            className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
              chartMode === "yearly"
                ? "bg-amber-400 text-zinc-900 font-medium"
                : "text-zinc-300 hover:bg-zinc-700"
            }`}
          >
            Theo năm
          </button>
        </div>
      </div>

      <Line data={data} options={options} />

      {chartMode === "yearly" && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mt-5">
          <div className="bg-zinc-800/70 rounded-2xl p-3">
            <p className="text-zinc-400 text-xs">
              Doanh thu năm 2026
            </p>

            <p className="text-lg font-semibold text-white mt-1">
              8.2 tỷ đ
            </p>
          </div>

          <div className="bg-zinc-800/70 rounded-2xl p-3">
            <p className="text-zinc-400 text-xs">
              Tăng trưởng YoY
            </p>

            <p className="text-lg font-semibold text-emerald-400 mt-1">
              +10.8%
            </p>
          </div>

          <div className="bg-zinc-800/70 rounded-2xl p-3">
            <p className="text-zinc-400 text-xs">
              Năm cao nhất
            </p>

            <p className="text-lg font-semibold text-amber-300 mt-1">
              2026
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
