"""
CircleGuard Performance Test Suite
===================================
Simulates realistic load patterns across the 6 core microservices.

Usage:
  locust -f locustfile.py --host http://localhost:8180
  locust -f locustfile.py --headless -u 100 -r 10 -t 5m --html reports/locust_report.html
"""

import json
import random
import time
import uuid
from locust import HttpUser, TaskSet, task, between, events
from locust.contrib.fasthttp import FastHttpUser


# ─── Auth helpers ──────────────────────────────────────────────────────────────

AUTH_URL        = "http://localhost:8180"
FORM_URL        = "http://localhost:8086"
PROMOTION_URL   = "http://localhost:8088"
DASHBOARD_URL   = "http://localhost:8084"

CREDENTIALS = [
    {"username": "admin", "password": "admin123"},
]

SYMPTOM_COMBOS = [
    {"hasFever": True,  "hasCough": True,  "otherSymptoms": "fatigue"},
    {"hasFever": True,  "hasCough": False, "otherSymptoms": ""},
    {"hasFever": False, "hasCough": True,  "otherSymptoms": "sore throat"},
    {"hasFever": False, "hasCough": False, "otherSymptoms": ""},
]

DEPARTMENTS = ["CS", "MATH", "BIO", "CHEM", "ENG", "ARTS"]


def login(client) -> str | None:
    """Attempts login and returns JWT token, or None on failure."""
    creds = random.choice(CREDENTIALS)
    with client.post(
        f"{AUTH_URL}/api/v1/auth/login",
        json=creds,
        catch_response=True,
        name="[auth] POST /api/v1/auth/login"
    ) as resp:
        if resp.status_code == 200:
            return resp.json().get("token")
        resp.failure(f"Login failed: {resp.status_code}")
        return None


# ─── Task Sets ─────────────────────────────────────────────────────────────────

class SurveyTasks(TaskSet):
    """Simulates students submitting health surveys (heaviest load)."""

    token: str = ""

    def on_start(self):
        tok = login(self.client)
        if tok:
            self.token = tok

    def auth_header(self):
        return {"Authorization": f"Bearer {self.token}"}

    @task(5)
    def submit_survey(self):
        if not self.token:
            return
        symptoms = random.choice(SYMPTOM_COMBOS)
        anon_id = str(uuid.uuid4())
        payload = {"anonymousId": anon_id, **symptoms}
        self.client.post(
            f"{FORM_URL}/api/v1/surveys",
            json=payload,
            headers=self.auth_header(),
            name="[form] POST /api/v1/surveys"
        )

    @task(2)
    def list_surveys(self):
        if not self.token:
            return
        self.client.get(
            f"{FORM_URL}/api/v1/surveys",
            headers=self.auth_header(),
            name="[form] GET /api/v1/surveys"
        )

    @task(1)
    def check_pending_surveys(self):
        if not self.token:
            return
        self.client.get(
            f"{FORM_URL}/api/v1/surveys/pending",
            headers=self.auth_header(),
            name="[form] GET /api/v1/surveys/pending"
        )


class HealthStatusTasks(TaskSet):
    """Simulates health officers checking status (medium load)."""

    token: str = ""

    def on_start(self):
        tok = login(self.client)
        if tok:
            self.token = tok

    def auth_header(self):
        return {"Authorization": f"Bearer {self.token}"}

    @task(4)
    def get_health_stats(self):
        if not self.token:
            return
        self.client.get(
            f"{PROMOTION_URL}/api/v1/health-status/stats",
            headers=self.auth_header(),
            name="[promotion] GET /api/v1/health-status/stats"
        )

    @task(2)
    def get_department_stats(self):
        if not self.token:
            return
        dept = random.choice(DEPARTMENTS)
        self.client.get(
            f"{PROMOTION_URL}/api/v1/health-status/stats/department/{dept}",
            headers=self.auth_header(),
            name="[promotion] GET /api/v1/health-status/stats/department/{dept}"
        )

    @task(1)
    def get_circles(self):
        if not self.token:
            return
        anon_id = str(uuid.uuid4())
        self.client.get(
            f"{PROMOTION_URL}/api/v1/circles/user/{anon_id}",
            headers=self.auth_header(),
            name="[promotion] GET /api/v1/circles/user/{id}"
        )


class DashboardTasks(TaskSet):
    """Simulates admin dashboards and analytics (lightest load)."""

    token: str = ""

    def on_start(self):
        tok = login(self.client)
        if tok:
            self.token = tok

    def auth_header(self):
        return {"Authorization": f"Bearer {self.token}"}

    @task(3)
    def campus_summary(self):
        if not self.token:
            return
        self.client.get(
            f"{DASHBOARD_URL}/api/v1/analytics/summary",
            headers=self.auth_header(),
            name="[dashboard] GET /api/v1/analytics/summary"
        )

    @task(2)
    def time_series_hourly(self):
        if not self.token:
            return
        self.client.get(
            f"{DASHBOARD_URL}/api/v1/analytics/timeseries?period=hourly&limit=24",
            headers=self.auth_header(),
            name="[dashboard] GET /api/v1/analytics/timeseries"
        )

    @task(1)
    def department_stats(self):
        if not self.token:
            return
        dept = random.choice(DEPARTMENTS)
        self.client.get(
            f"{DASHBOARD_URL}/api/v1/analytics/department/{dept}",
            headers=self.auth_header(),
            name="[dashboard] GET /api/v1/analytics/department/{dept}"
        )


# ─── User classes ──────────────────────────────────────────────────────────────

class StudentUser(HttpUser):
    """Represents a student submitting surveys (70% of traffic)."""
    tasks = [SurveyTasks]
    weight = 7
    wait_time = between(1, 3)
    host = AUTH_URL


class HealthOfficerUser(HttpUser):
    """Represents health officers checking promotion status (20% of traffic)."""
    tasks = [HealthStatusTasks]
    weight = 2
    wait_time = between(2, 5)
    host = AUTH_URL


class AdminUser(HttpUser):
    """Represents admins using the dashboard (10% of traffic)."""
    tasks = [DashboardTasks]
    weight = 1
    wait_time = between(5, 10)
    host = AUTH_URL


# ─── Event hooks for custom metrics ────────────────────────────────────────────

@events.request.add_listener
def on_request(request_type, name, response_time, response_length, **kwargs):
    """Can be extended for custom metrics export (Prometheus, InfluxDB, etc.)."""
    pass
