import { Injectable } from '@nestjs/common';
import * as nodemailer from 'nodemailer';

@Injectable()
export class MailService {
  private transporter: nodemailer.Transporter;

  constructor() {
    this.transporter = nodemailer.createTransport({
      host: 'smtp.qq.com',
      port: 465,
      secure: true,
      auth: {
        user: process.env.MAIL_USER,
        pass: process.env.MAIL_PASS,
      },
    });
  }

  async sendResetCode(to: string, code: string) {
    await this.transporter.sendMail({
      from: `"黑胶档案馆" <${process.env.MAIL_USER}>`,
      to,
      subject: '密码重置验证码',
      html: `
        <div style="max-width:480px;margin:0 auto;padding:32px;font-family:-apple-system,sans-serif">
          <h2 style="color:#c49333;margin-bottom:16px">黑胶档案馆</h2>
          <p style="color:#333;font-size:15px">您正在重置密码，验证码如下：</p>
          <div style="background:#f5f5f7;border-radius:12px;padding:24px;text-align:center;margin:24px 0">
            <span style="font-size:36px;font-weight:700;letter-spacing:8px;color:#1d1d1f">${code}</span>
          </div>
          <p style="color:#86868b;font-size:13px">验证码 90 秒内有效。如非本人操作，请忽略此邮件。</p>
        </div>
      `,
    });
  }
}
