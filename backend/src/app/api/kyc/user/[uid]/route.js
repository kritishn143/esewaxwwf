import { NextResponse } from 'next/server';
import path from 'path';

const dataFile = path.join(process.cwd(), 'kyc_data.json');

export async function GET(request, { params }) {
  try {
    const { uid } = await params;
    const currentData = JSON.parse(require('fs').readFileSync(dataFile, 'utf8'));
    
    // Find the latest record for this uid
    const record = currentData.find(item => item.uid === uid);
    
    if (!record) {
      return NextResponse.json({ success: false, error: 'Not found' }, { status: 404 });
    }

    return NextResponse.json(record);
  } catch (error) {
    console.error('API Error:', error);
    return NextResponse.json({ success: false, error: error.message }, { status: 500 });
  }
}
