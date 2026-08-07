export default function TermsPage() {
  return (
    <div style={{
      background: "#000", minHeight: "100vh", color: "#e8e8e6",
      fontFamily: "'Inter', system-ui, sans-serif",
    }}>
      <div style={{ maxWidth: "760px", margin: "0 auto", padding: "80px 40px" }}>
        <h1 style={{ fontSize: "32px", fontWeight: 700, letterSpacing: "-0.02em", marginBottom: "8px" }}>
          Terms of Service
        </h1>
        <p style={{ fontSize: "13px", color: "rgba(232,232,230,0.4)", marginBottom: "48px" }}>
          Last updated: August 7, 2026
        </p>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>1. Acceptance of Terms</h2>
          <p style={p}>
            By accessing or using Zyntral AI (&quot;the Service&quot;), you agree to be bound by these Terms of Service.
            If you do not agree to these terms, do not use the Service.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>2. Description of Service</h2>
          <p style={p}>
            Zyntral AI is an AI-powered content management platform that allows users to generate, schedule,
            and publish content to social media accounts. The Service is provided by OreoStudios, an
            individual entrepreneur registered in France.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>3. User Accounts</h2>
          <p style={p}>
            You are responsible for maintaining the confidentiality of your account credentials and for all
            activities that occur under your account. You must notify us immediately of any unauthorized use.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>4. Acceptable Use</h2>
          <p style={p}>You agree not to use the Service to:</p>
          <ul style={ul}>
            <li style={li}>Post content that is illegal, harmful, or violates any third-party rights</li>
            <li style={li}>Spam, harass, or abuse other users or third parties</li>
            <li style={li}>Circumvent any security or access controls</li>
            <li style={li}>Use the Service in violation of any applicable laws or regulations</li>
          </ul>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>5. Social Media Integrations</h2>
          <p style={p}>
            When you connect a social media account, you authorize Zyntral AI to publish content on your
            behalf. You remain solely responsible for all content posted through the Service. You must comply
            with the terms of service of each connected platform.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>6. AI-Generated Content</h2>
          <p style={p}>
            The Service uses AI models to generate content suggestions. You are responsible for reviewing
            and approving all AI-generated content before publication. Zyntral AI does not guarantee the
            accuracy, quality, or appropriateness of generated content.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>7. Intellectual Property</h2>
          <p style={p}>
            You retain ownership of all content you create or upload. By using the Service, you grant
            Zyntral AI a limited license to process and publish your content as directed by you.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>8. Limitation of Liability</h2>
          <p style={p}>
            The Service is provided &quot;as is&quot; without warranties of any kind. OreoStudios shall not be liable
            for any indirect, incidental, or consequential damages arising from your use of the Service.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>9. Termination</h2>
          <p style={p}>
            We reserve the right to suspend or terminate your account at any time for violation of these
            terms. You may delete your account at any time from the Settings page.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>10. Governing Law</h2>
          <p style={p}>
            These Terms are governed by the laws of France. Any disputes shall be subject to the exclusive
            jurisdiction of the courts of France.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>11. Contact</h2>
          <p style={p}>
            For any questions regarding these Terms, contact us at:{" "}
            <a href="mailto:legal@zyntralai.com" style={{ color: "#818cf8" }}>legal@zyntralai.com</a>
          </p>
        </section>

        <div style={{
          borderTop: "1px solid rgba(255,255,255,0.08)",
          paddingTop: "24px",
          fontSize: "12px",
          color: "rgba(232,232,230,0.3)",
          lineHeight: 1.8,
        }}>
          <p>© 2026 OreoStudios — Web & Game Development Studio</p>
          <p>SIREN 993 823 459 · SIRET 993 823 459 00017 · Code APE 62.01Z · Entrepreneur individuel — France</p>
        </div>
      </div>
    </div>
  );
}

const h2: React.CSSProperties = {
  fontSize: "16px", fontWeight: 600, marginBottom: "12px",
  color: "#e8e8e6", letterSpacing: "-0.01em",
};
const p: React.CSSProperties = {
  fontSize: "14px", lineHeight: 1.7, color: "rgba(232,232,230,0.6)", marginBottom: "12px",
};
const ul: React.CSSProperties = {
  paddingLeft: "20px", margin: "8px 0",
};
const li: React.CSSProperties = {
  fontSize: "14px", lineHeight: 1.7, color: "rgba(232,232,230,0.6)", marginBottom: "4px",
};
