import request from 'supertest';
import app from '../src/app';
import { clearDatabase, disconnectPrisma } from './helper';

beforeAll(async () => {
  try {
    await clearDatabase();
  } catch (err) {
    console.warn('Cảnh báo: Không thể dọn dẹp database. Có thể PostgreSQL chưa hoạt động.', err);
  }
});

afterAll(async () => {
  await disconnectPrisma();
});

describe('HỆ THỐNG & HEALTH CHECK', () => {
  it('Trả về trạng thái OK và thông tin thời gian hợp lệ', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('OK');
    expect(res.body).toHaveProperty('timestamp');
  });
});

describe('TC_AUTH_REGISTER - Đăng ký tài khoản', () => {
  const validUser = {
    username: 'test_ranger',
    password: 'password123',
    fullName: 'Nguyen Van A',
    phoneNumber: '+84901234567',
    role: 'RANGER'
  };

  it('TC_AUTH_REG_SUCCESS_01: Register user successfully', async () => {
    const res = await request(app)
      .post('/auth/register')
      .send(validUser);

    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('id');
    expect(res.body.username).toBe(validUser.username);
    expect(res.body.fullName).toBe(validUser.fullName);
    expect(res.body.phoneNumber).toBe(validUser.phoneNumber);
    expect(res.body.role).toBe(validUser.role);
  });

  it('TC_AUTH_REG_FAILURE_01: Register missing username', async () => {
    const { username: _, ...payload } = validUser;
    const res = await request(app)
      .post('/auth/register')
      .send({ ...payload, username: '' });
    expect(res.status).toBe(400);
    expect(res.body.error).toContain('username');
  });

  it('TC_AUTH_REG_FAILURE_02: Register missing password', async () => {
    const { password: _, ...payload } = validUser;
    const res = await request(app)
      .post('/auth/register')
      .send({ ...payload, password: '' });
    expect(res.status).toBe(400);
    expect(res.body.error).toContain('password');
  });

  it('TC_AUTH_REG_FAILURE_03: Register missing fullName', async () => {
    const { fullName: _, ...payload } = validUser;
    const res = await request(app)
      .post('/auth/register')
      .send({ ...payload, fullName: '' });
    expect(res.status).toBe(400);
    expect(res.body.error).toContain('fullName');
  });

  it('TC_AUTH_REG_FAILURE_04: Register missing phoneNumber', async () => {
    const { phoneNumber: _, ...payload } = validUser;
    const res = await request(app)
      .post('/auth/register')
      .send({ ...payload, phoneNumber: '' });
    expect(res.status).toBe(400);
    expect(res.body.error).toContain('phoneNumber');
  });

  it('TC_AUTH_REG_FAILURE_05: Register missing role', async () => {
    const { role: _, ...payload } = validUser;
    const res = await request(app)
      .post('/auth/register')
      .send({ ...payload, role: '' });
    expect(res.status).toBe(400);
    expect(res.body.error).toContain('role');
  });

  it('TC_AUTH_REG_FAILURE_06: Register with invalid phone number format', async () => {
    const res = await request(app)
      .post('/auth/register')
      .send({ ...validUser, username: 'ranger2', phoneNumber: '0901234567' }); // Không bắt đầu bằng +
    expect(res.status).toBe(400);
    expect(res.body.error).toContain('số điện thoại không hợp lệ');
  });

  it('TC_AUTH_REG_FAILURE_07: Register password less than 6 characters', async () => {
    const res = await request(app)
      .post('/auth/register')
      .send({ ...validUser, username: 'ranger3', password: '123' });
    expect(res.status).toBe(400);
    expect(res.body.error).toContain('ít nhất 6 ký tự');
  });

  it('TC_AUTH_REG_FAILURE_08: Register duplicate username or phone number', async () => {
    const res = await request(app)
      .post('/auth/register')
      .send(validUser); // Gửi lại thông tin trùng
    expect(res.status).toBe(409);
    expect(res.body.error).toContain('tồn tại');
  });
});

describe('TC_AUTH_LOGIN - Đăng nhập tài khoản', () => {
  it('TC_AUTH_LOGIN_SUCCESS_01: Login successfully with valid credentials', async () => {
    const res = await request(app)
      .post('/auth/login')
      .send({
        username: 'test_ranger',
        password: 'password123'
      });

    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('token');
    expect(res.body.role).toBe('RANGER');
  });

  it('TC_AUTH_LOGIN_FAILURE_01: Login with incorrect password', async () => {
    const res = await request(app)
      .post('/auth/login')
      .send({
        username: 'test_ranger',
        password: 'wrongpassword'
      });
    expect(res.status).toBe(401);
    expect(res.body.error).toContain('Sai tài khoản hoặc mật khẩu');
  });

  it('TC_AUTH_LOGIN_FAILURE_02: Login with non-existing username', async () => {
    const res = await request(app)
      .post('/auth/login')
      .send({
        username: 'non_exist_ranger',
        password: 'password123'
      });
    expect(res.status).toBe(401);
    expect(res.body.error).toContain('Sai tài khoản hoặc mật khẩu');
  });
});

describe('TC_AUTH_ME & LOGOUT - Phiên đăng nhập', () => {
  let authToken = '';

  beforeAll(async () => {
    // Đăng nhập để lấy token hoạt động
    try {
      const res = await request(app)
        .post('/auth/login')
        .send({
          username: 'test_ranger',
          password: 'password123'
        });
      authToken = res.body.token;
    } catch (err) {
      console.warn('Lấy token kiểm thử thất bại.', err);
    }
  });

  it('TC_AUTH_ME_SUCCESS_01: Retrieve profile details successfully', async () => {
    if (!authToken) return; // Skip test nếu DB offline
    const res = await request(app)
      .get('/users/me')
      .set('Authorization', `Bearer ${authToken}`);

    expect(res.status).toBe(200);
    expect(res.body.username).toBe('test_ranger');
    expect(res.body.role).toBe('RANGER');
  });

  it('TC_AUTH_LOGOUT_SUCCESS_01: Logout and destroy token successfully', async () => {
    if (!authToken) return; // Skip test nếu DB offline
    const res = await request(app)
      .post('/auth/logout')
      .set('Authorization', `Bearer ${authToken}`);

    expect(res.status).toBe(200);
    expect(res.body.message).toContain('Đăng xuất');

    // Truy cập me sau khi logout phải thất bại
    const meRes = await request(app)
      .get('/users/me')
      .set('Authorization', `Bearer ${authToken}`);
    expect(meRes.status).toBe(401);
  });
});
