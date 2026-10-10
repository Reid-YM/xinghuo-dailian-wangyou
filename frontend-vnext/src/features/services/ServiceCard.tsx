import { ArrowRight, Clock3 } from 'lucide-react';
import { Badge, Card } from '../../components/ui';
import {
  serviceCategory,
  serviceCoverImage,
  serviceDescription,
  serviceDuration,
  serviceName,
  servicePrice,
  serviceSlug,
  type ServiceRecord,
} from './model';

export function ServiceCard({ service }: { service: ServiceRecord }) {
  const slug = serviceSlug(service);
  const image = serviceCoverImage(service);
  const href = slug ? `/service/${encodeURIComponent(slug)}` : '/quote';

  return (
    <Card className="v-service-card">
      {image && <a className="v-service-cover" href={href}><img src={image} alt={serviceName(service)} loading="lazy" /></a>}
      <div className="v-service-card-top"><Badge>{serviceCategory(service)}</Badge><Clock3 size={18} /></div>
      <h3>{serviceName(service)}</h3>
      <p>{serviceDescription(service)}</p>
      <div className="v-service-price"><strong>{servicePrice(service)}</strong><span>{serviceDuration(service)}</span></div>
      <a className="v-service-link" href={href}>查看详情 <ArrowRight size={16} /></a>
    </Card>
  );
}
