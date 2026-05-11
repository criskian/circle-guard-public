"""
CircleGuard Spike Test
=======================
Simulates a sudden surge of users (e.g., morning campus arrival).
Ramps from 10 to 500 users in 30 seconds, holds for 2 minutes, then drops.

Run:
  locust -f locustfile_spike.py --headless -u 500 -r 50 -t 3m --html reports/spike_report.html
"""

from locust import HttpUser, task, between, LoadTestShape
import random, uuid


SYMPTOM_COMBOS = [
    {"hasFever": True,  "hasCough": True},
    {"hasFever": False, "hasCough": False},
]


class SpikeUser(HttpUser):
    host = "http://localhost:8180"
    wait_time = between(0.5, 1.5)

    _token: str = ""

    def on_start(self):
        resp = self.client.post("/api/v1/auth/login",
                                json={"username": "admin", "password": "admin123"},
                                name="[spike] login")
        if resp.status_code == 200:
            self._token = resp.json().get("token", "")

    @task(8)
    def submit_survey(self):
        if not self._token:
            return
        self.client.post(
            "http://localhost:8086/api/v1/surveys",
            json={**random.choice(SYMPTOM_COMBOS), "anonymousId": str(uuid.uuid4())},
            headers={"Authorization": f"Bearer {self._token}"},
            name="[spike] submit_survey"
        )

    @task(2)
    def get_stats(self):
        if not self._token:
            return
        self.client.get(
            "http://localhost:8088/api/v1/health-status/stats",
            headers={"Authorization": f"Bearer {self._token}"},
            name="[spike] get_stats"
        )


class SpikeShape(LoadTestShape):
    """
    Spike profile:
      0-30s:  ramp from 10 → 500
      30-150s: hold at 500
      150-180s: drop to 10
    """
    stages = [
        {"duration": 30,  "users": 500,  "spawn_rate": 50},
        {"duration": 150, "users": 500,  "spawn_rate": 50},
        {"duration": 180, "users": 10,   "spawn_rate": 50},
    ]

    def tick(self):
        run_time = self.get_run_time()
        for stage in self.stages:
            if run_time < stage["duration"]:
                tick_data = (stage["users"], stage["spawn_rate"])
                return tick_data
        return None
