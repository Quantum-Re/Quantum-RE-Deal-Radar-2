# Forecasting — Google TimesFM 3.0

Previsione zero-shot di serie numeriche (prezzi €/m², volumi aste, ROI, ...) con
[TimesFM 3.0](https://huggingface.co/google/timesfm-3.0-pytorch) di Google Research.

> ⚠️ **Licenza**: il codice è Apache-2.0, ma i **pesi di TimesFM 3.0** sono sotto
> *TimesFM Non-Commercial License v1.0* (uso non commerciale / non in produzione).
> Per uso commerciale: TimesFM 2.5 (pesi Apache-2.0) oppure TimesFM via Google Cloud (BigQuery ML).

## Installazione

```bash
cd packages/forecasting
python3 -m venv .venv
.venv/bin/pip install torch --index-url https://download.pytorch.org/whl/cpu   # oppure torch CUDA se hai una GPU
.venv/bin/pip install -r requirements.txt
```

Al primo avvio i pesi (~1.2 GB) vengono scaricati da Hugging Face e messi in cache.

## Uso

```bash
.venv/bin/python forecast.py examples/prezzo_mq_milano.csv --horizon 6
```

Il CSV deve avere una colonna numerica (`value`, configurabile con `--value-col`) ordinata nel tempo
e, opzionalmente, una colonna `date`. L'output contiene la previsione mediana (`forecast`) e
l'intervallo all'80% (`p10`–`p90`), salvato in `<input>.forecast.csv`.

Da Python:

```python
from forecast import load_model, forecast
model = load_model()
df = forecast(model, values, horizon=12)
```
