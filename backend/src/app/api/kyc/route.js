import { NextResponse } from 'next/server';
import path from 'path';

// We'll store data in a simple JSON file
const dataFile = path.join(process.cwd(), 'kyc_data.json');
const uploadsDir = path.join(process.cwd(), 'public', 'uploads');

// Ensure directories exist
if (!require('fs').existsSync(uploadsDir)) {
  require('fs').mkdirSync(uploadsDir, { recursive: true });
}
if (!require('fs').existsSync(dataFile)) {
  require('fs').writeFileSync(dataFile, JSON.stringify([]));
}

export async function POST(request) {
  try {
    const formData = await request.formData();
    
    let currentData = [];
    if (require('fs').existsSync(dataFile)) {
      currentData = JSON.parse(require('fs').readFileSync(dataFile, 'utf8'));
    }

    const uid = formData.get('uid') || 'anonymous';
    const existingIndex = currentData.findIndex(item => item.uid === uid && uid !== 'anonymous');

    const kycEntry = {
      id: existingIndex >= 0 ? currentData[existingIndex].id : Date.now().toString(),
      uid: uid,
      fullName: formData.get('fullName') || '',
      dob: formData.get('dob') || '',
      nin: formData.get('nin') || '',
      sex: formData.get('sex') || '',
      nationality: formData.get('nationality') || '',
      address: formData.get('address') || '',
      status: 'PENDING',
      submittedAt: new Date().toISOString(),
      frontImage: null,
      backImage: null,
      selfieImage: null,
      history: existingIndex >= 0 ? (currentData[existingIndex].history || []) : []
    };

    // Keep old images if new ones aren't uploaded
    if (existingIndex >= 0) {
      kycEntry.frontImage = currentData[existingIndex].frontImage;
      kycEntry.backImage = currentData[existingIndex].backImage;
      kycEntry.selfieImage = currentData[existingIndex].selfieImage;
    }

    // Handle files
    const files = ['frontImage', 'backImage', 'selfieImage'];
    for (const fileKey of files) {
      const file = formData.get(fileKey);
      if (file && file.size > 0) {
        const buffer = Buffer.from(await file.arrayBuffer());
        const filename = `${kycEntry.id}_${fileKey}.jpg`;
        const filepath = path.join(uploadsDir, filename);
        require('fs').writeFileSync(filepath, buffer);
        kycEntry[fileKey] = `/uploads/${filename}`;
      }
    }

    // Save to DB
    if (existingIndex >= 0) {
      currentData[existingIndex] = kycEntry;
    } else {
      currentData.unshift(kycEntry);
    }
    require('fs').writeFileSync(dataFile, JSON.stringify(currentData, null, 2));

    return NextResponse.json({ success: true, id: kycEntry.id });
  } catch (error) {
    console.error('API Error:', error);
    return NextResponse.json({ success: false, error: error.message }, { status: 500 });
  }
}

export async function GET() {
  try {
    const currentData = JSON.parse(require('fs').readFileSync(dataFile, 'utf8'));
    return NextResponse.json(currentData);
  } catch (error) {
    return NextResponse.json([], { status: 500 });
  }
}
