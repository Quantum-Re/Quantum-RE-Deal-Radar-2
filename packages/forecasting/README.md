# Forecasting — Google TimesFM 2.5

Previsione zero-shot di serie numeriche (prezzi €/m², volumi aste, ROI, ...) con
[TimesFM 2.5](https://huggingface.co/google/timesfm-2.5-200m-pytorch) (200M parametri) di Google Research.

**Licenza**: codice e pesi di TimesFM 2.5 sono Apache-2.0, quindi utilizzabili anche
commercialmente. (Non usiamo TimesFM 3.0: i suoi pesi sono sotto licenza non commerciale.)

## Installazione

```bash
cd packages/forecasting
python3 -m venv .venv
.venv/bin/pip install torch --index-url https://download.pytorch.org/whl/cpu   # oppure torch CUDA se hai una GPU
.venv/bin/pip install -r requirements.txt
```

Al primo avvio i pesi (~0.9 GB) vengono scaricati da Hugging Face e messi in cache.

## Uso

```bash
.venv/bin/python forecast.py examples/prezzo_mq_milano.csv --horizon 6
```

Il CSV deve avere una colonna numerica (`value`, configurabile con `--value-col`) ordinata nel tempo
e, opzionalmente, una colonna `date`. Vengono usati al massimo gli ultimi 1024 punti; orizzonte massimo 256. L'output contiene la previsione mediana (`forecast`) e
l'intervallo all'80% (`p10`–`p90`), salvato in `<input>.forecast.csv`.

Da Python:

```python
from forecast import load_model, forecast
model = load_model()
df = forecast(model, values, horizon=12)  # colonne: forecast, p10, p90
```
