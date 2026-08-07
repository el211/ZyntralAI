export default function PrivacyPage() {
  return (
    <div style={{
      background: "#000", minHeight: "100vh", color: "#e8e8e6",
      fontFamily: "'Inter', system-ui, sans-serif",
    }}>
      <div style={{ maxWidth: "760px", margin: "0 auto", padding: "80px 40px" }}>
        <h1 style={{ fontSize: "32px", fontWeight: 700, letterSpacing: "-0.02em", marginBottom: "8px" }}>
          Privacy Policy
        </h1>
        <p style={{ fontSize: "13px", color: "rgba(232,232,230,0.4)", marginBottom: "48px" }}>
          Last updated: August 7, 2026
        </p>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>1. Data Controller</h2>
          <p style={p}>
            The data controller is OreoStudios, an individual entrepreneur registered in France
            (SIREN 993 823 459, SIRET 993 823 459 00017, Code APE 62.01Z).
            Contact: <a href="mailto:privacy@zyntralai.com" style={{ color: "#818cf8" }}>privacy@zyntralai.com</a>
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>2. Data We Collect</h2>
          <p style={p}>We collect the following personal data:</p>
          <ul style={ul}>
            <li style={li}>Account information: email address and password (hashed)</li>
            <li style={li}>Social media account tokens when you connect a platform</li>
            <li style={li}>Content you create, upload, or generate through the Service</li>
            <li style={li}>Usage data: pages visited, features used, timestamps</li>
            <li style={li}>Technical data: IP address, browser type, device information</li>
          </ul>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>3. How We Use Your Data</h2>
          <p style={p}>We use your data to:</p>
          <ul style={ul}>
            <li style={li}>Provide and operate the Service</li>
            <li style={li}>Publish content to your connected social media accounts on your behalf</li>
            <li style={li}>Send transactional emails (account verification, password reset)</li>
            <li style={li}>Improve and develop the Service</li>
            <li style={li}>Comply with legal obligations</li>
          </ul>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>4. Legal Basis (GDPR)</h2>
          <p style={p}>
            We process your data based on: (a) contract performance — to deliver the Service you signed up
            for; (b) legitimate interests — to improve and secure the Service; (c) legal obligation — where
            required by law.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>5. Data Sharing</h2>
          <p style={p}>
            We do not sell your personal data. We share data only with:
          </p>
          <ul style={ul}>
            <li style={li}>Social media platforms you explicitly connect (LinkedIn, Twitter/X, TikTok, etc.)</li>
            <li style={li}>AI model providers (Anthropic, OpenAI, Google) to generate content — only the prompts you submit</li>
            <li style={li}>Infrastructure providers (hosting, email) under data processing agreements</li>
          </ul>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>6. Data Retention</h2>
          <p style={p}>
            We retain your data for as long as your account is active. Upon account deletion, your personal
            data is deleted within 30 days, except where retention is required by law.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>7. Your Rights</h2>
          <p style={p}>Under GDPR, you have the right to:</p>
          <ul style={ul}>
            <li style={li}>Access your personal data</li>
            <li style={li}>Rectify inaccurate data</li>
            <li style={li}>Request deletion of your data</li>
            <li style={li}>Restrict or object to processing</li>
            <li style={li}>Data portability</li>
            <li style={li}>Lodge a complaint with the CNIL (French data protection authority)</li>
          </ul>
          <p style={p}>
            To exercise your rights, contact us at:{" "}
            <a href="mailto:privacy@zyntralai.com" style={{ color: "#818cf8" }}>privacy@zyntralai.com</a>
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>8. Cookies</h2>
          <p style={p}>
            We use only strictly necessary cookies for authentication and session management. We do not use
            advertising or tracking cookies.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>9. Security</h2>
          <p style={p}>
            We implement industry-standard security measures including encrypted data transmission (HTTPS),
            hashed passwords, and access controls. No method of transmission over the internet is 100% secure.
          </p>
        </section>

        <section style={{ marginBottom: "36px" }}>
          <h2 style={h2}>10. Changes to This Policy</h2>
          <p style={p}>
            We may update this Privacy Policy from time to time. We will notify you of significant changes
            by email or through the Service. Continued use after changes constitutes acceptance.
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
