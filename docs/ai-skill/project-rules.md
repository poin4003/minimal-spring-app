# Project Rules

## Scope
- Khong dua `sample.sql` vao bo rule cua project.
- `sample.sql` chi la file tam, khong phai source-of-truth cho schema, migration, hay naming convention.
- Project nay huong toi stack toi gian: Spring Boot API + H2 + JobRunr.
- Khong dua Redis, Kafka, hay external service vao baseline cua du an nay.
- Migration khoi tao nen toi gian, uu tien phuc vu entity hien tai va khong seed data neu chua duoc yeu cau.

## Source Structure
- `src/main/java/com/app/config`: cac cau hinh dung chung cua Spring nhu security, jwt, cors, swagger, mapper, exception, settings, webmvc.
- `src/main/java/com/app/core`: cac thanh phan dung chung nhu base entity, constant, response wrapper, security object, sync infra, util.
- `src/main/java/com/app/features/<domain>`: code duoc chia theo domain nghiep vu.
- Ben trong moi feature, uu tien giu cac nhom thu muc quen thuoc:
  - `api/v1/controller`
  - `service` va `service/impl`
  - `repository` va `repository/spec` neu can filter dong
  - `entity`
  - `schema/payload`
  - `schema/result`
  - `schema/filter`
  - `enums`, `sync`, `worker`, `excel`, `cronjob` neu domain can
- `src/main/resources`: file config, logback, va Flyway migration trong `db/migration`.
- `src/test/java`: test code.

## Coding Direction
- Controller nen mong, chu yeu nhan request, validate, map response va dat permission annotation.
- Business logic nen nam o service layer.
- Truy van DB va filter theo tieu chi nen nam o repository/specification layer.
- DTO request/response/filter nen dat duoi `schema`.
- Constant dung chung nen dat duoi `core/constant`.
- Feature moi nen di theo cau truc `features/<domain>` thay vi rai code theo technical layer tren toan project.
- API nen giu version trong path, uu tien `api/v1`.
- Khi doi schema DB, uu tien tao migration trong `src/main/resources/db/migration`.
- Khong mang theo bang, seed, hay integration cua du an cu neu code hien tai khong con dung toi.

## Collaboration Rule With User
- Truoc khi thay doi code hoac file, AI phai show code de user xem truoc.
- Chi duoc apply thay doi sau khi user xac nhan bang cach noi `apply`.
- Neu user noi ro y nhu `apply lien`, `apply luon`, hoac the hien rang can sua ngay, AI moi duoc apply ngay trong turn do.
- Ngoai le nay khong ap dung cho viec doc source, phan tich, review, hoac de xuat huong sua.

## Open Sections For Future Rules
- Naming convention cho entity, DTO, endpoint, repository.
- Rule ve transaction va validation.
- Rule ve logging.
- Rule ve test.
- Rule ve import/export va background job.
