"""Forecast a numeric time series with Google TimesFM 3.0.

Usage:
    python forecast.py INPUT.csv --horizon 12 [--value-col value] [--date-col date] [--out OUT.csv]

INPUT.csv must contain one numeric column (default: "value"), ordered in time.
If a date column is present, future dates are extrapolated from its frequency.
Output: median forecast plus p10/p90 bounds (80% interval).
"""
import argparse
import sys

import numpy as np
import pandas as pd
from timesfm3 import ModelConfig, TimesFM3Evaluator

CHECKPOINT = "google/timesfm-3.0-pytorch"


def load_model(device: str | None = None) -> TimesFM3Evaluator:
    return TimesFM3Evaluator(ModelConfig(checkpoint_path=CHECKPOINT, device=device))


def forecast(model: TimesFM3Evaluator, values: np.ndarray, horizon: int) -> pd.DataFrame:
    out = next(iter(model.predict_batch(
        [values.astype(np.float32)], horizon=horizon,
        return_quantiles=True, use_symmetric_averaging=False,
    )))
    q = out.quantiles  # (horizon, 9) -> quantiles 0.1 ... 0.9
    return pd.DataFrame({"forecast": out.forecast, "p10": q[:, 0], "p90": q[:, 8]}).astype("float64")


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("input")
    p.add_argument("--horizon", type=int, default=12)
    p.add_argument("--value-col", default="value")
    p.add_argument("--date-col", default="date")
    p.add_argument("--device", default=None, help="cpu, cuda, ... (default: auto)")
    p.add_argument("--out", default=None)
    args = p.parse_args()

    df = pd.read_csv(args.input)
    if args.value_col not in df.columns:
        sys.exit(f"Column '{args.value_col}' not found. Available: {list(df.columns)}")
    values = pd.to_numeric(df[args.value_col], errors="coerce").interpolate().dropna().to_numpy()
    if len(values) < 8:
        sys.exit("Need at least 8 historical points to forecast.")

    result = forecast(load_model(args.device), values, args.horizon)

    if args.date_col in df.columns:
        dates = pd.to_datetime(df[args.date_col])
        freq = pd.infer_freq(dates) or (dates.iloc[-1] - dates.iloc[-2])
        result.insert(0, args.date_col, pd.date_range(dates.iloc[-1], periods=args.horizon + 1, freq=freq)[1:].date)

    out_path = args.out or args.input.rsplit(".", 1)[0] + ".forecast.csv"
    result.round(2).to_csv(out_path, index=False)
    print(result.round(2).to_string(index=False))
    print(f"\nSaved to {out_path}")


if __name__ == "__main__":
    main()
