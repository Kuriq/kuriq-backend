# kuriq-backend
## 🔧 주요 API

### Course API
- `POST /api/v1/courses/search` - 강좌 검색 (chromaDB 연동)

### Roadmap API
- `POST /api/v1/roadmap/generate` - 로드맵 생성
- `GET /api/v1/roadmap/{roadmapId}` - 로드맵 조회
- `PATCH /api/v1/roadmap/{roadmapId}/activate` - 로드맵 활성화

### Note API
- `POST /api/v1/notes` - 노트 생성
- `GET /api/v1/notes?courseId={id}` - 노트 조회
- `PUT /api/v1/notes/{noteId}` - 노트 저장
- `POST /api/v1/notes/{noteId}/ai-organize` - AI 노트 정리
