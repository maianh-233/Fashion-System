import Button from "../common/Button";
import { useState } from "react";
import { compactMoney, dayLabel, money, yearSummary } from "../../pages/admin/dashboardLogic";

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

/** Doanh thu đơn hoàn thành: {@code daily} 7 ngày gần nhất, {@code yearly} 5 năm gần nhất (dạng {label, revenue}). */
export default function RevenueChart({ daily = [], yearly = [] }) {
  const [chartMode, setChartMode] = useState("weekly");

  const points = chartMode === "weekly" ? daily : yearly;
  const summary = yearSummary(yearly);

  const data = {
    labels: points.map(point => (chartMode === "weekly" ? dayLabel(point.label) : point.label)),
    datasets: [
      {
        data: points.map(point => Number(point.revenue || 0)),
        borderColor: "#c08a45",
        backgroundColor: "rgba(192,138,69,0.12)",
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
      tooltip: {
        callbacks: {
          label: context => money(context.parsed.y),
        },
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        // Khi chưa có doanh thu, giữ thang tối thiểu để trục không chia thành các mốc lẻ 0,1 đồng.
        suggestedMax: 1000000,
        ticks: {
          precision: 0,
          color: "#a1a1aa",
          callback: value => compactMoney(value),
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
          <Button
            onClick={() => setChartMode("weekly")}
            className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
              chartMode === "weekly"
                ? "bg-amber-400 text-zinc-900 font-medium"
                : "text-zinc-300 hover:bg-zinc-700"
            }`}
          >
            7 ngày
          </Button>

          <Button
            onClick={() => setChartMode("yearly")}
            className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
              chartMode === "yearly"
                ? "bg-amber-400 text-zinc-900 font-medium"
                : "text-zinc-300 hover:bg-zinc-700"
            }`}
          >
            Theo năm
          </Button>
        </div>
      </div>

      <Line data={data} options={options} />

      {chartMode === "yearly" && summary && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mt-5">
          <div className="bg-zinc-800/70 rounded-2xl p-3">
            <p className="text-zinc-400 text-xs">
              Doanh thu năm {summary.year}
            </p>

            <p className="text-lg font-semibold text-white mt-1">
              {compactMoney(summary.revenue)} đ
            </p>
          </div>

          <div className="bg-zinc-800/70 rounded-2xl p-3">
            <p className="text-zinc-400 text-xs">
              Tăng trưởng so với năm trước
            </p>

            <p className={`text-lg font-semibold mt-1 ${summary.growth === null ? "text-zinc-400" : summary.growth >= 0 ? "text-emerald-400" : "text-red-400"}`}>
              {summary.growth === null ? "—" : `${summary.growth > 0 ? "+" : ""}${summary.growth.toLocaleString("vi-VN")}%`}
            </p>
          </div>

          <div className="bg-zinc-800/70 rounded-2xl p-3">
            <p className="text-zinc-400 text-xs">
              Năm cao nhất
            </p>

            <p className="text-lg font-semibold text-amber-300 mt-1">
              {summary.bestYear || "—"}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
