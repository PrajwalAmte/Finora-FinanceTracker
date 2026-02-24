import React from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Mail, Calendar, Clock, LogOut } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { useAuth } from '../utils/auth-context';
import { toast } from '../utils/notifications';
import { formatDate } from '../utils/formatters';

function InfoRow({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="flex items-start py-3 border-b border-neutral-100 dark:border-neutral-700 last:border-0">
      <div className="flex items-center w-40 shrink-0 text-neutral-500 dark:text-neutral-400 text-sm">
        <span className="mr-2">{icon}</span>
        {label}
      </div>
      <div className="text-sm text-neutral-900 dark:text-neutral-100 font-medium">{value}</div>
    </div>
  );
}

export const ProfilePage: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    toast.info('You have been signed out.');
    navigate('/login', { replace: true });
  };

  if (!user) return null;

  const initials = user.username.slice(0, 2).toUpperCase();

  return (
    <div className="max-w-2xl space-y-6">
      {/* Avatar + name header */}
      <Card>
        <div className="flex items-center space-x-5">
          <div className="w-16 h-16 rounded-full bg-primary-100 dark:bg-primary-900/40 flex items-center justify-center shrink-0">
            <span className="text-xl font-bold text-primary-700 dark:text-primary-300">
              {initials}
            </span>
          </div>
          <div className="flex-1 min-w-0">
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white truncate">
              {user.username}
            </h2>
            <p className="text-sm text-neutral-500 dark:text-neutral-400 truncate">{user.email}</p>
          </div>
        </div>
      </Card>

      {/* Account details */}
      <Card title="Account Details" icon={<User size={18} />}>
        <div>
          <InfoRow icon={<User size={15} />} label="Username" value={user.username} />
          <InfoRow icon={<Mail size={15} />} label="Email" value={user.email} />
        </div>
      </Card>

      {/* Activity */}
      <Card title="Activity" icon={<Clock size={18} />}>
        <div>
          <InfoRow
            icon={<Calendar size={15} />}
            label="Member since"
            value={formatDate(user.createdAt)}
          />
          <InfoRow
            icon={<Clock size={15} />}
            label="Last updated"
            value={formatDate(user.updatedAt)}
          />
          <InfoRow
            icon={<Clock size={15} />}
            label="Last login"
            value={user.lastLoginAt ? formatDate(user.lastLoginAt) : '—'}
          />
        </div>
      </Card>

      {/* Logout */}
      <div>
        <Button variant="danger" iconLeft={<LogOut size={16} />} onClick={handleLogout}>
          Sign out
        </Button>
      </div>
    </div>
  );
};
