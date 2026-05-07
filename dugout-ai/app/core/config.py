from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """환경변수 → AI_* prefix.

    예: AI_LOG_LEVEL=INFO, AI_MAX_RECOMMEND_CANDIDATES=20
    """

    log_level: str = "INFO"
    max_recommend_candidates: int = 20
    algorithm_timeout_seconds: int = 30

    model_config = SettingsConfigDict(env_prefix="AI_", env_file=".env", extra="ignore")


settings = Settings()
