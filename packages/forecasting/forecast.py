"""Forecast a numeric time series with Google TimesFM 2.5 (Apache-2.0 weights).

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
import timesfm

CHECKPOINT = "google/timesfm-2.5-200m-pytorch"
MAX_CONTEXT = 1024
MAX_HORIZON = 256


def load_model() -> timesfm.TimesFM_2p5_200M_torch:
    model = timesfm.TimesFM_2p5_200M_torch.from_pretrained(CHECKPOINT)
    model.compile(timesfm.ForecastConfig(
        max_context=MAX_CONTEXT,
        max_horizon=MAX_HORIZON,
        normalize_inputs=True,
        use_continuous_quantile_head=True,
        force_flip_invariance=True,
        infer_is_positive=True,
        fix_quantile_crossing=True,
    ))
    return model


def forecast(model: timesfm.TimesFM_2p5_200M_torch, values: np.ndarray, horizon: int) -> pd.DataFrame:
    _, quantiles = model.forecast(horizon=horizon, inputs=[values.astype(np.float32)])
    q = quantiles[0]  # (horizon, 10): mean, then quantiles 0.1 ... 0.9
    return pd.DataFrame({"forecast": q[:, 5], "p10": q[:, 1], "p90": q[:, 9]}).astype("float64")


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("input")
    p.add_argument("--horizon", type=int, default=12)
    p.add_argument("--value-col", default="value")
    p.add_argument("--date-col", default="date")
    p.add_argument("--out", default=None)
    args = p.parse_args()

    if not 1 <= args.horizon <= MAX_HORIZON:
        sys.exit(f"--horizon must be between 1 and {MAX_HORIZON}.")
    df = pd.read_csv(args.input)
    if args.value_col not in df.columns:
        sys.exit(f"Column '{args.value_col}' not found. Available: {list(df.columns)}")
    values = pd.to_numeric(df[args.value_col], errors="coerce").interpolate().dropna().to_numpy()
    if len(values) < 8:
        sys.exit("Need at least 8 historical points to forecast.")

    result = forecast(load_model(), values[-MAX_CONTEXT:], args.horizon)

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
