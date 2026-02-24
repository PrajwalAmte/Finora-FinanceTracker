import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { DollarSign, User, Mail, Lock } from 'lucide-react';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { ThemeToggle } from '../components/layout/ThemeToggle';
import { useAuth } from '../utils/auth-context';
import { toast } from '../utils/notifications';

interface RegisterErrors {
  username?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
}

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const { register } = useAuth();

  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<RegisterErrors>({});

  const validate = () => {
    const next: RegisterErrors = {};
    if (!username.trim()) next.username = 'Username is required';
    else if (username.trim().length < 3) next.username = 'Username must be at least 3 characters';
    if (!email.trim()) next.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) next.email = 'Enter a valid email address';
    if (!password) next.password = 'Password is required';
    else if (password.length < 8) next.password = 'Password must be at least 8 characters';
    if (!confirmPassword) next.confirmPassword = 'Please confirm your password';
    else if (password !== confirmPassword) next.confirmPassword = 'Passwords do not match';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setIsLoading(true);
    try {
      await register(username.trim(), email.trim(), password);
      toast.success('Account created! Welcome to Finora.');
      navigate('/', { replace: true });
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Registration failed. Please try again.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-neutral-50 dark:bg-neutral-950 flex flex-col">
      {/* Top bar */}
      <div className="flex items-center justify-between px-6 py-4">
        <div className="flex items-center space-x-2">
          <DollarSign className="text-primary-600 dark:text-primary-500" size={28} />
          <span className="text-xl font-bold text-neutral-900 dark:text-white">Finora</span>
        </div>
        <ThemeToggle />
      </div>

      {/* Centered form */}
      <div className="flex-1 flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-md">
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-primary-100 dark:bg-primary-900/30 mb-4">
              <DollarSign className="text-primary-600 dark:text-primary-400" size={32} />
            </div>
            <h1 className="text-2xl font-bold text-neutral-900 dark:text-white">Create your account</h1>
            <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">
              Start managing your finances with Finora
            </p>
          </div>

          <div className="bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-800 rounded-xl shadow-sm p-8">
            <form onSubmit={handleSubmit} className="space-y-5" noValidate>
              <Input
                label="Username"
                type="text"
                placeholder="Choose a username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                error={errors.username}
                icon={<User size={16} />}
                fullWidth
                autoComplete="username"
                autoFocus
              />

              <Input
                label="Email"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                error={errors.email}
                icon={<Mail size={16} />}
                fullWidth
                autoComplete="email"
              />

              <div className="space-y-1">
                <Input
                  label="Password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="At least 8 characters"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  error={errors.password}
                  icon={<Lock size={16} />}
                  fullWidth
                  autoComplete="new-password"
                />
                <div className="flex justify-end">
                  <button
                    type="button"
                    className="text-xs text-primary-600 dark:text-primary-400 hover:underline"
                    onClick={() => setShowPassword((v) => !v)}
                  >
                    {showPassword ? 'Hide password' : 'Show password'}
                  </button>
                </div>
              </div>

              <Input
                label="Confirm Password"
                type={showPassword ? 'text' : 'password'}
                placeholder="Re-enter your password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                error={errors.confirmPassword}
                icon={<Lock size={16} />}
                fullWidth
                autoComplete="new-password"
              />

              <Button type="submit" fullWidth isLoading={isLoading} size="lg">
                Create account
              </Button>
            </form>
          </div>

          <p className="mt-6 text-center text-sm text-neutral-500 dark:text-neutral-400">
            Already have an account?{' '}
            <Link
              to="/login"
              className="text-primary-600 dark:text-primary-400 font-medium hover:underline"
            >
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};
