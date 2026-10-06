# Solvix Portfolio Demo Deployment Plan

## Objective

The next deployment outcome is a public, interactive portfolio demo: a recruiter follows a link from the repository or portfolio, opens Solvix, and can explore a working ticket workflow. This is a small demonstration environment, not a production service, customer pilot, availability commitment, or proof that the AWS target architecture has been deployed.

Keep the separately documented AWS architecture as a future design and portfolio artifact. Do not provision the full AWS stack merely to keep a demo URL online.

## Cost constraint

The target is **$0 ongoing service spend**, using only free plans/allowances and stopping if a provider requires a paid upgrade or payment commitment. “Free” is conditional on provider eligibility, current terms, region, and usage quotas; it is not a promise of permanent availability or zero risk. Recheck provider pricing and terms immediately before deployment. Do not enter a paid plan or attach payment details as a workaround for exhausted allowances without an explicit decision.

The following is a candidate deployment for a personal, low-traffic demonstration, checked on September 30, 2026:

| Component | Candidate | Why it fits the demo | Important limits |
| --- | --- | --- | --- |
| Next.js frontend | Vercel Hobby | Free plan is documented for personal projects and small-scale applications; a public HTTPS project URL avoids needing a custom domain. | Hobby is for personal, non-commercial use and has monthly resource limits. Verify use complies with the current terms. |
| Spring Boot API | Render Free Web Service using the existing Java 17 Docker image | Can run a containerized API without operating a server. | Free services sleep after 15 minutes without inbound traffic; first request after sleeping takes about a minute. Free instance hours, bandwidth, and build minutes are limited. The API filesystem is ephemeral. |
| PostgreSQL | Neon Free | Persistent managed PostgreSQL avoids losing application data when the API container sleeps or restarts. The plan currently lists 0.5 GB storage and 100 compute-unit hours per project; compute scales to zero after inactivity. | Quotas, retention, and plan features can change. Free use is not an SLA or production backup/restore plan. Use a dedicated demo database containing synthetic data only. |

Official plan references: [Vercel Hobby](https://vercel.com/docs/plans/hobby), [Render free instances](https://render.com/docs/free), and [Neon plans](https://neon.com/docs/introduction/plans). Revalidate them at deployment time.

This hosting combination is a **candidate, not yet a deployed or guaranteed-free service**. Render's free API can experience cold starts, so show a clear “waking up”/retry state in the UI rather than presenting a timeout as an application error. Keep the API and frontend deployable elsewhere so that provider limits do not force a redesign.

## Public demo safety requirements

The current ticket UI and API were built for authenticated local users, not unrestricted public traffic. Do not deploy the existing support login or bootstrap credentials as a public demo.

Before publishing a live URL:

- Add a demo-specific access mode with server-enforced limits. Do not rely on hidden/disabled frontend controls as authorization.
- Use synthetic seed content only. Never load real tickets, personal data, AWS credentials, Cognito secrets, or local bootstrap secrets into the public demo.
- Do not expose a support-agent or administrator session, even as a shared demo password.
- Define which operations visitors can try. Keep assignment and privileged workflow actions disabled on the server; allow only the deliberately designed demo actions.
- Ensure one visitor cannot view or change another visitor's submitted data. Use isolated per-session demo data or a read-only catalogue; do not share one customer identity for all visitors.
- Bound request size and rates, validate all inputs, avoid logging secrets or unnecessary submitted content, and provide a safe reset/expiry policy for visitor-created demo data.
- Verify the demo against a fresh database, including migrations, synthetic seeding, access-denial tests, reset behavior, and browser smoke tests.
- Configure provider secrets only in the respective hosting dashboards. Keep `.env` files and credentials out of Git.
- Add a visible demo banner describing cold starts, synthetic data, and that the environment is not for real support requests.
- Add a reliable health endpoint and a UI recovery path for sleeping/unavailable API or database services.
- Recheck each provider's acceptable-use terms, free quotas, account eligibility, and billing configuration before deployment. Monitor usage and remove the deployment if it leaves the approved free allowance.

No live demo should be described as production-ready. Do not make sensitive data, availability, backup, security certification, or cost guarantees based only on a free hosting plan.

## Delivery sequence

1. **Finish the public demo contract:** choose read-only catalogue versus isolated visitor sandbox, define allowed actions, data reset/retention, and what the UI tells visitors.
2. **Implement demo isolation:** enforce the demo access model in the backend, use deterministic synthetic fixtures, and add authorization, abuse-limit, and reset tests.
3. **Prepare deployability:** configure Render with `PORT` (the API defaults to 8080 locally) and the standard Spring datasource overrides `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. Set the JDBC URL to Neon’s TLS-enabled connection string. Confirm migrations are safe on a clean Neon database and the API can recover from database scale-to-zero.
4. **Deploy the API and database:** link the repository's [`render.yaml`](../render.yaml) as a Render Blueprint, provide the listed runtime settings in Render's secret configuration, and use the linked Neon production branch for the JDBC connection. Check cold-start and health behavior.
5. **Deploy the frontend:** connect the repository to a Vercel Hobby project, configure the API URL and exact CORS origin, and use the generated HTTPS URL unless a domain is already available at no cost.
6. **Test as a stranger:** use a clean browser with no local state. Exercise the advertised ticket flows, cross-visitor access denial, refresh/recovery behavior, and mobile layout. Check provider usage after smoke testing.
7. **Publish cautiously:** add the verified live URL to the README only after smoke tests pass. Document that it is a free-tier portfolio demo, its cold-start behavior, synthetic-data policy, and the AWS architecture as a separate, not-yet-deployed target.
8. **Operate within the constraint:** periodically check quotas and terms; export any portfolio evidence that must be retained. If a provider changes its free offer or requires payment, pause/remove the demo or select another free option rather than silently upgrading.

## AWS target remains separate

The AWS target in the [production readiness roadmap](./07-production-readiness-roadmap.md) remains a future, separately costed design: AWS CDK, Amplify Hosting, a container on ECS/Fargate, private single-AZ RDS PostgreSQL, Cognito, Secrets Manager, and CloudWatch. It is not required for the portfolio URL and should not be provisioned under a no-paid-spend constraint without verifying account eligibility, service allowances, and estimated costs. S3 attachments, Bedrock, Multi-AZ, and always-on production infrastructure remain deferred.
